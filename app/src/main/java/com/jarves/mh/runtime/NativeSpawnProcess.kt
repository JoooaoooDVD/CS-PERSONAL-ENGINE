package com.jarves.mh.runtime

import android.os.ParcelFileDescriptor
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

internal class NativeSpawnProcess private constructor(
    private val pid: Int,
    internal val outputFile: File,
    private val stdinPfd: ParcelFileDescriptor,
    private val stdin: OutputStream,
    private val outputPump: Thread? = null,
) : Process() {
    @Volatile private var result: Int? = null

    override fun getOutputStream(): OutputStream = stdin
    override fun getInputStream(): InputStream = FileInputStream(outputFile)
    override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))

    override fun waitFor(): Int {
        result?.let { return it }
        return NativeSpawn.waitFor(pid, false).also {
            result = it
            outputPump?.join(1_000)
        }
    }

    override fun exitValue(): Int {
        result?.let { return it }
        val status = NativeSpawn.waitFor(pid, true)
        if (status == NativeSpawn.STILL_RUNNING) throw IllegalThreadStateException("Process is still running")
        return status.also { result = it }
    }

    override fun destroy() {
        NativeSpawn.kill(pid, 15)
    }

    /** Send the same interrupt signal produced by Ctrl+C in a real terminal. */
    internal fun interrupt() {
        NativeSpawn.kill(pid, 2)
    }

    /** Dynamically updates the kernel PTY window size (`TIOCSWINSZ` -> `SIGWINCH`). */
    internal fun resizePty(rows: Int, columns: Int) {
        runCatching {
            val fd = stdinPfd.fd
            if (fd >= 0) NativeSpawn.resizePty(fd, rows.coerceIn(10, 300), columns.coerceIn(20, 400))
        }
    }

    override fun destroyForcibly(): Process {
        NativeSpawn.kill(pid, 9)
        return this
    }

    override fun isAlive(): Boolean = runCatching { exitValue(); false }.getOrDefault(true)

    companion object {
        fun start(
            argv: List<String>,
            environment: Map<String, String>,
            cwd: String,
            outputFile: File,
            pseudoTerminal: Boolean = false,
            ptyRows: Int = 40,
            ptyColumns: Int = 120,
            onLivePtyChunk: ((ByteArray, Int) -> Unit)? = null,
        ): NativeSpawnProcess {
            outputFile.parentFile?.mkdirs()
            if (pseudoTerminal) outputFile.delete()
            val spawned = NativeSpawn.spawn(
                argv.toTypedArray(),
                environment.map { "${it.key}=${it.value}" }.toTypedArray(),
                cwd,
                outputFile.absolutePath,
                pseudoTerminal,
                ptyRows,
                ptyColumns,
            )
            check(spawned.size == 3 && spawned[0] > 0) { "Native runtime launch failed" }
            val adoptedStdinPfd = ParcelFileDescriptor.adoptFd(spawned[1])
            val input = ParcelFileDescriptor.AutoCloseOutputStream(adoptedStdinPfd)
            val pump = spawned[2].takeIf { it >= 0 }?.let { outputFd ->
                Thread({
                    runCatching {
                        ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.adoptFd(outputFd)).use { source ->
                            FileOutputStream(outputFile, false).use { destination ->
                                val buffer = ByteArray(16 * 1024)
                                while (true) {
                                    val readCount = source.read(buffer)
                                    if (readCount <= 0) break
                                    destination.write(buffer, 0, readCount)
                                    onLivePtyChunk?.invoke(buffer, readCount)
                                }
                            }
                        }
                    }
                }, "pocket-pty-output").apply {
                    isDaemon = true
                    priority = Thread.MAX_PRIORITY - 1
                    start()
                }
            }
            return NativeSpawnProcess(spawned[0], outputFile, adoptedStdinPfd, input, pump)
        }
    }
}

private object NativeSpawn {
    const val STILL_RUNNING = -2

    init {
        System.loadLibrary("pocketspawn")
    }

    external fun spawn(
        argv: Array<String>,
        environment: Array<String>,
        cwd: String,
        outputFile: String,
        pseudoTerminal: Boolean,
        ptyRows: Int,
        ptyColumns: Int,
    ): IntArray
    external fun waitFor(pid: Int, noHang: Boolean): Int
    external fun kill(pid: Int, signal: Int): Int
    external fun resizePty(fd: Int, rows: Int, columns: Int): Int
}
