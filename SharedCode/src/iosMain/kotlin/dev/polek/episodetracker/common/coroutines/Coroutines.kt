package dev.polek.episodetracker.common.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import platform.darwin.*
import kotlin.coroutines.CoroutineContext

actual val ui: CoroutineContext = MainQueueDispatcher()

actual val io: CoroutineContext = MainQueueDispatcher()

actual fun runBlocking(block: suspend () -> Unit) = runBlocking { block() }

@OptIn(InternalCoroutinesApi::class)
internal class MainQueueDispatcher: CoroutineDispatcher(), Delay {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch_async(dispatch_get_main_queue()) { block.run() }
    }

    @InternalCoroutinesApi
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
            with(continuation) { resumeUndispatched(Unit) }
        }
    }

    @InternalCoroutinesApi
    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
        val handle = object : DisposableHandle {
            var disposed = false
                private set

            override fun dispose() {
                disposed = true
            }
        }
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
            if (!handle.disposed) {
                block.run()
            }
        }

        return handle
    }
}
