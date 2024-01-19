package functions.executor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import functions.api.BitriseApiStation
import functions.api.SlackApiStation
import functions.cli.tokenizeArgs
import functions.env.Env
import functions.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ExecutorDeploy(
    private val payload: PubSubMessagePayload,
    private val workflowId: String,
) : Executor {

    companion object {
        const val DEFAULT_TYPE = "patch"
    }

    override suspend fun execute() {
        val command = object : CliktCommand() {
            val type by option(
                "-t", "--type",
                help = "The version name format is [major.minor.patch], and the type is used to determine which type of version name to adjust. Default: $DEFAULT_TYPE",
            ).default(DEFAULT_TYPE)

            val notify by option(
                "-n", "--notify",
                help = "Notify the Quality Assurance Team when the deploy is finished. Default: false"
            ).flag(default = false)

            override fun run() {}
        }
        try {
            command.parse(payload.text.tokenizeArgs())
        } catch (e: Exception) {
            if (e is PrintHelpMessage) {
                runBlocking {
                    SlackApiStation.respondEphemeral(
                        responseUrl = payload.responseUrl,
                        text = "```\n${e.command.getFormattedHelp()}\n```",
                    )
                }
            } else {
                runBlocking {
                    SlackApiStation.respondEphemeral(
                        responseUrl = payload.responseUrl,
                        text = "${e.message}. Use `--help` to see a list of all options.",
                    )
                }
            }
            return
        }

        withContext(Dispatchers.IO) {
            BitriseApiStation.triggerBuild(
                body = BitriseTriggerRequest(
                    hookInfo = BitriseTriggerRequest.HookInfo(
                        buildTriggerToken = System.getenv(Env.BITRISE_BUILD_TRIGGER_TOKEN),
                    ),
                    buildParams = BitriseTriggerRequest.BuildParams(
                        workflowId = workflowId,
                        environments = BitriseTriggerRequest.BuildParams.Environment.Builder()
                            .increaseVersionType(command.type)
                            .notifyQualityAssuranceTeamOrNot(command.notify)
                            .build(),
                    ),
                    triggeredBy = "${payload.userName} used Slack slash command to create a trigger url.",
                ),
            ).doOnSuccess { response ->
                val slackWebhooks = Json.parseToJsonElement(System.getenv(Env.SLACK_WEBHOOKS)).jsonObject
                val androidCommandWebhook = slackWebhooks["android_command"]?.contentOrNull!!
                SlackApiStation.respondInChannel(
                    responseUrl = androidCommandWebhook,
                    text = """
                        <@${payload.userId}> triggered <${response.buildUrl}|Bitrise build #${response.buildNumber}>.
                        > Workflow: `${response.triggeredWorkflow}`
                    """.trimIndent(),
                )
            }.doOnFailure {
                SlackApiStation.respondEphemeral(
                    responseUrl = payload.responseUrl,
                    text = "Bitrise responded with error: ```${it.message}```",
                )
            }
        }
    }
}