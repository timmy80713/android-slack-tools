package functions.model.slack

import functions.model.clickup.ClickUpTask
import kotlinx.serialization.json.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SlackMessagePayloadCreator {

    private val notDisplayedInStore: String
        get() {
            return """
                *不顯示於商店*
            """.trimIndent()
        }

    private val publishInfo: String
        get() {
            val currentDate = OffsetDateTime.now()
                .atZoneSameInstant(ZoneId.of("Asia/Taipei"))
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            return """
                > Taiwan Google Play 正式發布：$currentDate
                > Taiwan X 發布：$currentDate
                > Overseas Google Play 正式發布：$currentDate
            """.trimIndent()
        }

    fun createChangelog(
        tag: String,
        clickUpTasks: List<ClickUpTask>,
        slackUsers: List<SlackUser>,
    ): JsonObject {
        return buildJsonObject {
            put("response_type", "in_channel")
            put("blocks", buildJsonArray {
                add(generateSlackBlockSection {
                    val text = """
                        *${tag}*
                    """.trimIndent()
                    generateSlackBlockMarkdown(text)
                })
                add(generateSlackBlockSection { generateSlackBlockMarkdown(publishInfo) })
                add(generateSlackBlockSection { generateSlackBlockMarkdown(notDisplayedInStore) })
                add(
                    generateSlackBlockTasks(
                        clickUpTasks = clickUpTasks,
                        slackUsers = slackUsers,
                        withoutUser = false,
                    )
                )
            })
        }
    }

    fun createChangelogWithoutUser(
        tag: String,
        clickUpTasks: List<ClickUpTask>,
        slackUsers: List<SlackUser>,
    ): JsonObject {
        return buildJsonObject {
            put("response_type", "in_channel")
            put("blocks", buildJsonArray {
                add(generateSlackBlockSection {
                    val text = """
                        *${tag}*
                    """.trimIndent()
                    generateSlackBlockMarkdown(text)
                })
                add(generateSlackBlockSection { generateSlackBlockMarkdown(publishInfo) })
                add(generateSlackBlockSection { generateSlackBlockMarkdown(notDisplayedInStore) })
                add(
                    generateSlackBlockTasks(
                        clickUpTasks = clickUpTasks,
                        slackUsers = slackUsers,
                        withoutUser = true,
                    )
                )
            })
        }
    }

    private fun generateSlackBlockTasks(
        clickUpTasks: List<ClickUpTask>,
        slackUsers: List<SlackUser>,
        withoutUser: Boolean,
    ): JsonObject {
        val richTextSections = clickUpTasks.map { task ->
            generateSlackBlockRichTextSection {
                buildJsonArray {
                    add(generateSlackBlockLink(text = task.name, url = task.url))
                    add(generateSlackBlockText(text = " "))
                    if (withoutUser.not()) {
                        add(generateSlackBlockText(text = "cc "))
                        val taskRelatedUsers = listOf(task.creator) + task.assignees
                        taskRelatedUsers.distinctBy { it.email }.forEach { taskRelatedUser ->
                            slackUsers
                                .find { user -> user.profile.email == taskRelatedUser.email }
                                ?.also { add(generateSlackBlockUser(it.id)) }
                            add(generateSlackBlockText(text = " "))
                        }
                    }
                }
            }
        }

        return generateSlackBlockRichText {
            JsonArray(listOf(generateSlackBlockRichTextList { JsonArray(richTextSections) }))
        }
    }

    private fun generateSlackBlockSection(text: () -> JsonObject): JsonObject {
        return buildJsonObject {
            put("type", "section")
            put("text", text.invoke())
        }
    }

    private fun generateSlackBlockRichText(elements: () -> JsonArray): JsonObject {
        return buildJsonObject {
            put("type", "rich_text")
            put("elements", elements())
        }
    }

    private fun generateSlackBlockRichTextList(
        style: String = "bullet",
        elements: () -> JsonArray,
    ): JsonObject {
        return buildJsonObject {
            put("type", "rich_text_list")
            put("elements", elements())
            put("style", style)
        }
    }

    private fun generateSlackBlockRichTextSection(elements: () -> JsonArray): JsonObject {
        return buildJsonObject {
            put("type", "rich_text_section")
            put("elements", elements())
        }
    }

    private fun generateSlackBlockLink(text: String, url: String): JsonObject {
        return buildJsonObject {
            put("type", "link")
            put("text", text)
            put("url", url)
        }
    }

    private fun generateSlackBlockUser(userId: String): JsonObject {
        return buildJsonObject {
            put("type", "user")
            put("user_id", userId)
        }
    }

    private fun generateSlackBlockMarkdown(text: String): JsonObject {
        return buildJsonObject {
            put("type", "mrkdwn")
            put("text", text)
        }
    }

    private fun generateSlackBlockText(text: String): JsonObject {
        return buildJsonObject {
            put("type", "text")
            put("text", text)
        }
    }
}