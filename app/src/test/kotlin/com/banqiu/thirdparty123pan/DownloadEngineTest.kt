package com.banqiu.thirdparty123pan

import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.data.db.entity.TransferTaskEntity
import com.banqiu.thirdparty123pan.data.transfer.*
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DownloadEngineTest {
    private class Fixture : AutoCloseable {
        val folder = Files.createTempDirectory("cloud123-download-test").toFile()
        val file = File(folder, "task.part")
        val server = MockWebServer().apply { start() }
        val dao = MemoryDao(TransferTaskEntity(id = 1, name = "download.bin", size = 6, storageKind = "public"))
        var published = 0
        var failPublish = false
        val storage = object : DownloadTargetStorage {
            override fun stagingFile(task: TransferTaskEntity) = file
            override fun validateDirectory(directory: String) {}
            override fun directoryLabel(directory: String) = directory
            override suspend fun checkConflict(task: TransferTaskEntity) {}
            override suspend fun publish(taskId: Long) {
                if (failPublish) throw IOException("simulated provider failure")
                published++
                dao.task = dao.task.copy(phase = "published")
            }
            override suspend fun discardOutput(task: TransferTaskEntity) {}
            override suspend fun cleanup(task: TransferTaskEntity) {}
        }
        val repo = Proxy.newProxyInstance(FileRepository::class.java.classLoader, arrayOf(FileRepository::class.java)) { _, method, _ ->
            if (method.name == "resolveDownloadUrl") server.url("/download").toString() else error("Unexpected call: ${method.name}")
        } as FileRepository
        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build()
        val engine = DownloadEngine(client, dao, repo, storage)
        override fun close() {
            server.shutdown(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown()
            folder.deleteRecursively()
        }
    }

    @Test fun completeDownloadPublishesExactBytes() = runBlocking {
        Fixture().use { f ->
            f.server.enqueue(MockResponse().setBody("abcdef"))
            f.engine.download(1) { _, _ -> }
            assertEquals("abcdef", f.file.readText())
            assertEquals(1, f.published)
            assertEquals("published", f.dao.task.phase)
        }
    }
    @Test fun rangeResponseAppendsOnlyExpectedBytes() = runBlocking {
        Fixture().use { f ->
            f.file.writeText("abc")
            f.server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range", "bytes 3-5/6").setBody("def"))
            f.engine.download(1) { _, _ -> }
            assertEquals("bytes=3-", f.server.takeRequest().getHeader("Range"))
            assertEquals("abcdef", f.file.readText())
        }
    }
    @Test fun ignoredRangeTruncatesBeforeRestart() = runBlocking {
        Fixture().use { f ->
            f.file.writeText("xxx")
            f.server.enqueue(MockResponse().setBody("abcdef"))
            f.engine.download(1) { _, _ -> }
            assertEquals("abcdef", f.file.readText())
        }
    }
    @Test fun invalidRangeLeavesPartialFileUntouched() = runBlocking {
        Fixture().use { f ->
            f.file.writeText("abc")
            f.server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range", "bytes 2-5/6").setBody("cdef"))
            try { f.engine.download(1) { _, _ -> }; fail("accepted wrong offset") } catch (_: IOException) {}
            assertEquals("abc", f.file.readText()); assertEquals(0, f.published)
        }
    }
    @Test fun prematureEofNeverPublishes() = runBlocking {
        Fixture().use { f ->
            f.server.enqueue(MockResponse().setBody("abc"))
            try { f.engine.download(1) { _, _ -> }; fail("accepted incomplete file") } catch (_: IOException) {}
            assertEquals(0, f.published); assertEquals("abc", f.file.readText())
        }
    }
    @Test fun providerFailureRetainsStagingAndRetrySkipsNetwork() = runBlocking {
        Fixture().use { f ->
            f.failPublish = true
            f.server.enqueue(MockResponse().setBody("abcdef"))
            try { f.engine.download(1) { _, _ -> }; fail("provider error lost") } catch (_: IOException) {}
            assertEquals("abcdef", f.file.readText()); assertEquals("downloaded", f.dao.task.phase)
            f.failPublish = false
            f.engine.download(1) { _, _ -> }
            assertEquals(1, f.server.requestCount); assertEquals(1, f.published)
        }
    }
    @Test fun pauseCancelsBlockedSocketWithoutPublishing() = runBlocking {
        Fixture().use { f ->
            f.server.enqueue(MockResponse().setBody("abcdef").setBodyDelay(3, TimeUnit.SECONDS))
            val job = launch(Dispatchers.IO) { f.engine.download(1) { _, _ -> } }
            withContext(Dispatchers.IO) { assertNotNull(f.server.takeRequest(2, TimeUnit.SECONDS)) }
            withTimeout(2000) { job.cancelAndJoin() }
            assertEquals(0, f.published)
        }
    }
}

private class MemoryDao(var task: TransferTaskEntity) : TransferTaskDao {
    override fun observeAll(): Flow<List<TransferTaskEntity>> = flowOf(listOf(task))
    override suspend fun getById(id: Long) = task.takeIf { it.id == id }
    override suspend fun getByStatuses(statuses: List<Int>) = listOf(task).filter { it.status in statuses }
    override suspend fun insert(task: TransferTaskEntity): Long { this.task = task; return task.id }
    override suspend fun update(task: TransferTaskEntity) { this.task = task }
    override suspend fun updateStatus(id: Long, status: Int) { task = task.copy(status = status) }
    override suspend fun updateProgress(id: Long, progress: Float, speed: Long) { task = task.copy(progress = progress, speed = speed) }
    override suspend fun finish(id: Long, status: Int, finishTime: Long) { task = task.copy(status = status, finishTime = finishTime) }
    override suspend fun updateError(id: Long, error: String?) { task = task.copy(error = error) }
    override suspend fun updateUrl(id: Long, url: String?) { task = task.copy(url = url) }
    override suspend fun delete(id: Long) {}
    override suspend fun clearFinished() {}
}
