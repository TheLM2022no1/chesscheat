package com.example.chesscheat

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object BoardReader {

    private const val BOARD_LEFT   = 0.05f
    private const val BOARD_TOP    = 0.10f
    private const val BOARD_RIGHT  = 0.95f
    private const val BOARD_BOTTOM = 0.85f

    private var templates: Map<Char, Mat> = emptyMap()

    fun loadTemplates(ctx: android.content.Context) {
        val map = mutableMapOf<Char, Mat>()
        val pieces = "KQRBNPkqrbnp"
        for (p in pieces) {
            val name = "piece_${pieceName(p)}"
            val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
            if (resId == 0) continue
            val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, resId) ?: continue
            val mat = Mat(); Utils.bitmapToMat(bmp, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.resize(mat, mat, Size(64.0, 64.0))
            map[p] = mat
        }
        templates = map
    }

    private fun pieceName(p: Char): String = when (p) {
        'K' -> "wk"; 'Q' -> "wq"; 'R' -> "wr"; 'B' -> "wb"; 'N' -> "wn"; 'P' -> "wp"
        'k' -> "bk"; 'q' -> "bq"; 'r' -> "br"; 'b' -> "bb"; 'n' -> "bn"; 'p' -> "bp"
        else -> "unknown"
    }

    fun readBoard(bitmap: Bitmap): Array<Array<Char?>> {
        val mat = Mat(); Utils.bitmapToMat(bitmap, mat)
        val w = mat.cols(); val h = mat.rows()

        val left   = (w * BOARD_LEFT).toInt()
        val top    = (h * BOARD_TOP).toInt()
        val right  = (w * BOARD_RIGHT).toInt()
        val bottom = (h * BOARD_BOTTOM).toInt()

        val boardW = right - left
        val boardH = bottom - top
        val cellW = boardW / 8.0
        val cellH = boardH / 8.0

        val board = Array(8) { arrayOfNulls<Char>(8) }

        for (r in 0 until 8) for (c in 0 until 8) {
            val roi = Rect(
                (left + c * cellW).toInt(),
                (top + r * cellH).toInt(),
                cellW.toInt(),
                cellH.toInt()
            )
            if (roi.x + roi.width > w || roi.y + roi.height > h) continue
            val cell = Mat(mat, roi)
            board[r][c] = classifyCell(cell)
        }
        return board
    }

    private fun classifyCell(cell: Mat): Char? {
        if (templates.isEmpty()) return null
        val gray = Mat(); Imgproc.cvtColor(cell, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.resize(gray, gray, Size(64.0, 64.0))

        val mean = MatOfDouble(); val stddev = MatOfDouble()
        Core.meanStdDev(gray, mean, stddev)
        if (stddev.get(0, 0)[0] < 15.0) return null

        var bestPiece: Char? = null
        var bestScore = 0.5
        for ((p, tpl) in templates) {
            val res = Mat()
            Imgproc.matchTemplate(gray, tpl, res, Imgproc.TM_CCOEFF_NORMED)
            val mm = Core.minMaxLoc(res)
            if (mm.maxVal > bestScore) { bestScore = mm.maxVal; bestPiece = p }
        }
        return bestPiece
    }
}
