package functions.executor

import functions.model.clickup.ClickUpCustomField
import functions.model.clickup.ClickUpStatus
import functions.model.clickup.ClickUpView
import functions.model.resultOrNull
import functions.repo.ClickUpRepoImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.logging.Logger

class ExecutorRegressionHotfix(
    private val tag: String,
    private val clickUpRepoImpl: ClickUpRepoImpl,
) : Executor {

    private val logger = Logger.getLogger(ExecutorRegressionHotfix::class.java.name)

    override suspend fun execute() {

        val clickUpView = ClickUpView.RegressionHotfix
        val targetStatus = ClickUpStatus.RegressionTest

        val clickUpTasks = clickUpRepoImpl.fetchTasks(clickUpView.id).filter {
            it.name.contains("TimmmmmmY", ignoreCase = true)
        }

        logger.info("${clickUpView.name} has ${clickUpTasks.size} tasks.")

        withContext(Dispatchers.IO) {
            clickUpTasks.map {
                async { clickUpRepoImpl.addCustomField(it.id, ClickUpCustomField.AndroidAppVersion.id, tag) }
            }.awaitAll().mapNotNull { it.resultOrNull() }
            clickUpTasks.map {
                async { clickUpRepoImpl.updateTask(it.id, targetStatus) }
            }.awaitAll().mapNotNull { it.resultOrNull() }
        }
    }
}