package functions.circleci

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import functions.App
import functions.cli.tokenizeArgs
import functions.env.Env
import functions.env.EnvironmentVariableNotSetException
import functions.extension.debugMessage
import functions.model.PubSubMessagePayload
import functions.model.doOnFailure
import functions.model.doOnSuccess
import functions.slack.SlackApi
import functions.slack.wrappedInCodeBlock
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

object CircleCi {

    class Command : CliktCommand() {

        val branch by option(
            "-b", "--branch",
            help = "The Git branch to build.",
        ).required()

        val variants by option(
            "-v", "--variants",
            help = "Variants to build e.g. dcardDevRelease. Select multiple with delimiter ','.",
        ).split(",").required()

        val message by option(
            "-m", "--message",
            help = "Message to display in Slack when the workflow is finished. " + "Default: ${createDefaultMessage("<User ID>")}",
        )

        override fun run() {}
    }

    private val logger = Logger.getLogger(App::class.java.name)

    fun handleRequest(payload: PubSubMessagePayload) {
        val command = Command().apply {
            try {
                parse(payload.text.tokenizeArgs())
            } catch (e: Exception) {
                logger.info(e.debugMessage)
                runBlocking {
                    if (e is PrintHelpMessage) {
                        SlackApi.respondEphemeral(
                            responseUrl = payload.responseUrl,
                            text = e.command.getFormattedHelp().wrappedInCodeBlock(),
                        )
                    } else {
                        SlackApi.respondEphemeralError(
                            responseUrl = payload.responseUrl,
                            message = "${e.message}. Use `--help` to see a list of all options.",
                        )
                    }
                }
                return
            }
        }
        val token = Env.CIRCLECI_PERSONAL_API_TOKEN.let {
            Env.get(it) ?: run {
                runBlocking {
                    SlackApi.respondEphemeralError(
                        responseUrl = payload.responseUrl,
                        message = EnvironmentVariableNotSetException(it).message!!,
                    )
                }
                return
            }
        }
        runBlocking {
            CircleCiApi.triggerBuild(
                token = token, requestBody = CircleCiTriggerRequest(
                    branch = command.branch, parameters = CircleCiTriggerRequest.Parameters(
                        pullRequestTrigger = false,
                        qaTrigger = true,
                        variants = command.variants.joinToString("\\n"),
                        message = command.message ?: createDefaultMessage("<@${payload.userId}>")
                    )
                )
            ).doOnSuccess {
                val url = "https://app.circleci.com/pipelines/github/timmy80713/android-dcard-sandbox/${it.number}"
                SlackApi.respondInChannel(
                    responseUrl = payload.responseUrl,
                    text = """
                        <@${payload.userId}> triggered <${url}|CircleCI build #${it.number}>.
                        > Git branch: `${command.branch}`
                        > Variants: ${command.variants.joinToString(" ") { "`$it`" }}
                    """.trimIndent()
                )
            }.doOnFailure {
                SlackApi.respondEphemeral(
                    responseUrl = payload.responseUrl,
                    text = "Bitrise responded with error: ```${it.debugMessage}```",
                )
            }
        }
    }

    private fun createDefaultMessage(authorName: String) = "Build triggered by $authorName."
}