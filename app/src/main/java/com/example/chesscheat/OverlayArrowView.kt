package com.example.chesscheat

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class OverlayArrowView(ctx: Context) : View(ctx) {

    private var bestMove: String? = null

    private val boardLeft   = 0.05f
    private val boardTop    = 0.10f
    private val boardRight  = 0.95f
    private val boardBottom = 0.85f

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 30, 200, 255)
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 30, 200, 255)
        style = Paint.Style.FILL
    }

    fun setMove(uci: String?) { bestMove = uci; postInvalidate() }

    private fun sqToPoint(sq: String): PointF {
        val file = sq[0] - 'a'
        val rank = sq[1] - '1'
        val row = 7 - rank
        val col = file

        val bw = (boardRight - boardLeft) * width
        val bh = (boardBottom - boardTop) * height
        val cw = bw / 8f
        val ch = bh / 8f
        val x = width * boardLeft + col * cw + cw / 2f
        val y = height * boardTop + row * ch + ch / 2f
        return PointF(x, y)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mv = bestMove ?: return
        if (width == 0 || height == 0) return
        if (mv.length < 4) return

        val from = sqToPoint(mv.substring(0, 2))
        val to   = sqToPoint(mv.substring(2, 4))
        drawArrow(canvas, from, to)
    }

    private fun drawArrow(c: Canvas, from: PointF, to: PointF) {
        val dx = to.x - from.x; val dy = to.y - from.y
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len < 20f) return
        val ux = dx / len; val uy = dy / len
        val headLen = minOf(len * 0.3f, 80f)
        val shaftEndX = to.x - ux * headLen * 0.9f
        val shaftEndY = to.y - uy * headLen * 0.9f
        c.drawLine(from.x, from.y, shaftEndX, shaftEndY, arrowPaint)

        val angle = atan2(uy.toDouble(), ux.toDouble())
        val wing = Math.PI / 7
        val path = Path().apply {
            moveTo(to.x, to.y)
            lineTo(to.x - headLen * cos(angle - wing).toFloat(),
                   to.y - headLen * sin(angle - wing).toFloat())
            lineTo(to.x - headLen * cos(angle + wing).toFloat(),
                   to.y - headLen * sin(angle + wing).toFloat())
            close()
        }
        c.drawPath(path, fillPaint)
        c.drawPath(path, arrowPaint)
    }
}
