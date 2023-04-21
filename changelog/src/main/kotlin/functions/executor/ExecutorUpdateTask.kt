package functions.executor

import functions.model.clickup.ClickUpStatus
import functions.model.clickup.ClickUpView
import functions.model.resultOrNull
import functions.repo.ClickUpRepoImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.logging.Logger

class ExecutorUpdateTask(
    private val clickUpView: ClickUpView,
    private val targetStatus: ClickUpStatus,
    private val clickUpRepoImpl: ClickUpRepoImpl,
) : Executor {

    private val logger = Logger.getLogger(ExecutorUpdateTask::class.java.name)

    override suspend fun execute() {
        val clickUpTasks = clickUpRepoImpl.fetchTasks(clickUpView.id).filter {
            it.name.contains("TimmmmmmY", ignoreCase = true)
        }

        logger.info("UpdateTask tasks size => ${clickUpTasks.size}")

        withContext(Dispatchers.IO) {
            clickUpTasks.map {
                async { clickUpRepoImpl.updateTask(it.id, targetStatus) }
            }.awaitAll().mapNotNull { it.resultOrNull() }
        }
    }
}