package functions.executor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import functions.api.BitriseApiStation
import functions.api.SlackApiStation
import functions.cli.tokenizeArgs
import functions.env.Env
import functions.model.BitriseTriggerRequest
import functions.model.PubSubMessagePayload
import functions.model.doOnFailure
import functions.model.doOnSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ExecutorBuildApp(
    private val payload: PubSubMessagePayload,
    private val workflowId: String,
) : Executor {

    override suspend fun execute() {
        val command = object : CliktCommand() {
            val branch by option(
                "-b", "--branch",
                help = "The Git branch to build.",
            ).required()

            val variants by option(
                "-v", "--variants",
                help = "Variants to build e.g. taiwanDevDebug. Select multiple with delimiter ','.",
            ).split(",").required()

            val appendMessage by option(
                "-m", "--message",
                help = "The default message will be \"${createDefaultMessage()}\", followed by your message.",
            )

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
                        workflowId = workflowId,
                        environments = BitriseTriggerRequest.BuildParams.Environment.Builder()
                            .buildVariants(command.variants)
                            .slackMessage(
                                StringBuilder(createDefaultMessage("<@${payload.userId}>"))
                                    .append(command.appendMessage
                                        ?.replace("\\n", "\n")
                                        ?.let { "\n${it}" } ?: ""
                                    )
                                    .toString()
                            )
                            .build(),
                    ),
                    triggeredBy = "${payload.userName} used Slack slash command to create a trigger url.",
                ),
            ).doOnSuccess { response ->
                SlackApiStation.respondInChannel(
                    responseUrl = payload.responseUrl,
                    text = """
                        <@${payload.userId}> triggered <${response.buildUrl}|Bitrise build #${response.buildNumber}>.
                        > Git branch: `${command.branch}`
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

    private fun createDefaultMessage(authorName: String = "<User ID>") = "Build triggered by $authorName."
}