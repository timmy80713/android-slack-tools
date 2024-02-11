package functions.executor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
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

class ExecutorBuildApp(
    private val payload: PubSubMessagePayload,
    private val workflowId: String,
) : Executor {

    override suspend fun execute() {
        val command = object : CliktCommand() {
            val branch by option(
                "-b", "--branch",
                help = "The Git branch to build.",
            )

            val tag by option(
                "-t", "--tag",
                help = "The Git tag to build.",
            )

            val commitHash by option(
                "-h", "--hash",
                help = "The Git commit hash to build.",
            )

            val variants by option(
                "-v", "--variants",
                help = "Variants to build e.g. taiwanDevDebug. Select multiple with delimiter ','.",
            ).split(",").required()

            val message by option(
                "-m", "--message",
                help = "Message will be appended on the next line after the section.",
            ).default("")

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
                        branch = command.branch,
                        tag = command.tag,
                        commitHash = command.commitHash,
                        workflowId = workflowId,
                        environments = BitriseTriggerRequest.BuildParams.Environment.Builder()
                            .buildVariants(command.variants)
                            .slackMessage(command.message)
                            .triggeredUser(payload.userId)
                            .build(),
                    ),
                    triggeredBy = "${payload.userName} used Slack slash command to create a trigger url.",
                ),
            ).doOnSuccess { response ->
                val target = if (command.commitHash != null) {
                    "Git commit hash: `${command.commitHash}`"
                } else if (command.tag != null) {
                    "Git tag: `${command.tag}`"
                } else {
                    if (command.branch == null) {
                        "Git branch: `Bitrise default branch`"
                    } else {
                        "Git branch: `${command.branch}`"
                    }
                }
                val slackWebhooks = Json.parseToJsonElement(System.getenv(Env.SLACK_WEBHOOKS)).jsonObject
                val androidCommandWebhook = slackWebhooks["android_command"]?.contentOrNull!!
                SlackApiStation.respondInChannel(
                    responseUrl = androidCommandWebhook,
                    text = """
                        <@${payload.userId}> triggered <${response.buildUrl}|Build #${response.buildNumber}>.
                        > ${target}
                        > Workflow: `${response.triggeredWorkflow}`
                        > Variants: ${command.variants.joinToString(" ") { "`$it`" }}
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