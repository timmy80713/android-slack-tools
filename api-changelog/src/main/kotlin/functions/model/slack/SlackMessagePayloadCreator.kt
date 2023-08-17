package functions.model.slack

import functions.model.clickup.ClickUpSpace
import functions.model.clickup.ClickUpTask
import kotlinx.serialization.json.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SlackMessagePayloadCreator {

    fun createChangelog(
        tag: String,
        spaces: List<ClickUpSpace>,
        taskGroups: Map<String, List<ClickUpTask>>,
        slackUsers: List<SlackUser>,
    ): JsonObject {
        return buildJsonObject {
            put("response_type", "in_channel")
            put("blocks", buildJsonArray {
                add(generateSlackBlockHeader { generateSlackBlockPlainText("A new version has been published.") })
                add(generateSlackBlockSection {
                    val currentDate = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("Asia/Taipei"))
                        .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    val text = """
                        > Version : *${tag}*
                        > Date : *${currentDate}*
                    """.trimIndent()
                    generateSlackBlockMarkdown(text)
                })

                add(generateSlackBlockHeader { generateSlackBlockPlainText("What changes are included in this version?") })
                val taskGroupsIterator = taskGroups.iterator()
                while (taskGroupsIterator.hasNext()) {
                    val map = taskGroupsIterator.next()
                    val (spaceId, tasks) = map
                    generateChanges(spaces, spaceId, tasks, slackUsers).forEach { add(it) }
                    if (taskGroupsIterator.hasNext()) {
                        add(generateSlackBlockDivider())
                    }
                }
            })
        }
    }

    private fun generateChanges(
        spaces: List<ClickUpSpace>,
        spaceId: String,
        tasks: List<ClickUpTask>,
        slackUsers: List<SlackUser>,
    ): List<JsonObject> {

        val jsonObjects = mutableListOf<JsonObject>()

        spaces.find { it.id == spaceId }?.also { space ->
            val name = space.name!!
            val avatar = space.avatar
            jsonObjects.add(generateSlackBlockContext {
                buildJsonArray {
                    if (avatar == null) {
                        add(generateSlackBlockMarkdown(":clickup: *${name}*"))
                    } else {
                        add(generateSlackBlockImage(avatar, name))
                        add(generateSlackBlockMarkdown("*${name}*"))
                    }
                }
            })
        }

        tasks.mapIndexed { index, task ->
            generateSlackBlockSection {
                val creator = slackUsers
                    .filter { slackUser -> slackUser.profile.email == task.creator.email }
                    .joinToString { slackUser -> "<@${slackUser.id}>" }

                val assignees = task.assignees
                    .mapNotNull { clickUpUser -> slackUsers.find { slackUser -> slackUser.profile.email == clickUpUser.email } }
                    .joinToString { slackUser -> "<@${slackUser.id}>" }

                val text = """
                    ><${task.url}|${task.name}>
                    > Creator : $creator
                    > Assignees : $assignees
                """.trimIndent()
                generateSlackBlockMarkdown(text)
            }
        }.let {
            jsonObjects.addAll(it)
        }

        return jsonObjects
    }

    private fun generateSlackBlockContext(elements: () -> JsonArray): JsonObject {
        return buildJsonObject {
            put("type", "context")
            put("elements", elements())
        }
    }

    private fun generateSlackBlockHeader(textObject: () -> JsonObject): JsonObject {
        return buildJsonObject {
            put("type", "header")
            put("text", textObject())
        }
    }

    private fun generateSlackBlockSection(textObject: () -> JsonObject): JsonObject {
        return buildJsonObject {
            put("type", "section")
            put("text", textObject())
        }
    }

    private fun generateSlackBlockPlainText(text: String): JsonObject {
        return buildJsonObject {
            put("type", "plain_text")
            put("text", text)
        }
    }

    private fun generateSlackBlockMarkdown(text: String): JsonObject {
        return buildJsonObject {
            put("type", "mrkdwn")
            put("text", text)
        }
    }

    private fun generateSlackBlockImage(imageUrl: String, altText: String): JsonObject {
        return buildJsonObject {
            put("type", "image")
            put("image_url", imageUrl)
            put("alt_text", altText)
        }
    }

    private fun generateSlackBlockDivider(): JsonObject {
        return buildJsonObject {
            put("type", "divider")
        }
    }
}