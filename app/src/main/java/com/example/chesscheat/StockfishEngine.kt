package com.example.chesscheat

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class StockfishEngine(private val ctx: Context) {

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    fun start() {
        val binFile = File(ctx.filesDir, "stockfish")
        if (!binFile.exists()) {
            try {
                ctx.assets.open("stockfish").use { input ->
                    binFile.outputStream().use { out -> input.copyTo(out) }
                }
                binFile.setExecutable(true)
            } catch (_: Throwable) { return }
        }

        val pb = ProcessBuilder(binFile.absolutePath)
        pb.redirectErrorStream(true)
        process = pb.start()
        writer = OutputStreamWriter(process!!.outputStream)
        reader = BufferedReader(InputStreamReader(process!!.inputStream))

        send("uci")
        while (true) {
            val line = reader?.readLine() ?: break
            if (line == "uciok") break
        }
    }

    fun send(cmd: String) {
        try {
            writer?.apply { write("$cmd\n"); flush() }
        } catch (_: Throwable) {}
    }

    fun bestMove(fen: String, depth: Int = 18, movetimeMs: Long = 1500): String? {
        send("position fen $fen")
        send("go depth $depth movetime $movetimeMs")
        while (true) {
            val line = reader?.readLine() ?: return null
            if (line.startsWith("bestmove")) {
                return line.split(" ").getOrNull(1)?.takeIf { it != "(none)" }
            }
        }
    }

    fun stop() {
        send("quit")
        try {
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (_: Throwable) {}
    }
}
