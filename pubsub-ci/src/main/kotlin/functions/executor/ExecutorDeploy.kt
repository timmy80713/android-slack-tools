package functions.executor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import functions.cli.tokenizeArgs
import functions.env.Env
import functions.model.PubSubMessagePayload
import functions.model.bitrise.BitriseTriggerRequest
import functions.model.contentOrNull
import functions.model.doOnFailure
import functions.model.doOnSuccess
import functions.repo.BitriseRepoImpl
import functions.repo.SlackRepoImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ExecutorDeploy(
    private val payload: PubSubMessagePayload,
    private val workflowId: String,
    private val bitriseRepoImpl: BitriseRepoImpl,
    private val slackRepoImpl: SlackRepoImpl,
) : Executor {

    companion object {
        const val DEFAULT_TYPE = "patch"
    }

    override suspend fun execute() {
        val command = object : CliktCommand() {

            init {
                context {
                    terminal = Terminal(theme = Theme.Plain)
                }
            }

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
            e.printStackTrace()
            if (e is CliktError) {
                slackRepoImpl.respondEphemeral(
                    webhookUrl = payload.responseUrl,
                    text = "```\n${command.getFormattedHelp(e)}\n```"
                )
            } else {
                slackRepoImpl.respondEphemeral(
                    webhookUrl = payload.responseUrl,
                    text = "Unknown exception, ${e.message}",
                )
            }
            return
        }

        withContext(Dispatchers.IO) {
            bitriseRepoImpl.triggerBuild(
                requestBody = BitriseTriggerRequest(
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
                slackRepoImpl.respondInChannel(
                    webhookUrl = androidCommandWebhook,
                    text = """
                        <@${payload.userId}> triggered <${response.buildUrl}|Bitrise build #${response.buildNumber}>.
                        > Workflow: `${response.triggeredWorkflow}`
                    """.trimIndent(),
                )
            }.doOnFailure {
                slackRepoImpl.respondEphemeral(
                    webhookUrl = payload.responseUrl,
                    text = "Bitrise responded with error: ```${it.message}```",
                )
            }
        }
    }
}