package com.example.chesscheat

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var statusView: TextView
    private var overlayView: OverlayArrowView? = null
    private var overlayAdded = false

    private lateinit var engine: StockfishEngine
    private val handler = Handler(Looper.getMainLooper())
    private var lastFen: String? = null
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!OpenCVLoader.initLocal()) { finish(); return }

        BoardReader.loadTemplates(this)
        engine = StockfishEngine(this).also { it.start() }
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }
        statusView = TextView(this).apply {
            text = "Status: idle"; textSize = 20f; setTextColor(Color.BLACK)
        }
        val btnOverlay = Button(this).apply {
            text = "ขอสิทธิ์ overlay"
            setOnClickListener { requestOverlayPermission() }
        }
        val btnStart = Button(this).apply {
            text = "Start"
            setOnClickListener { startCapture() }
        }
        val btnStop = Button(this).apply {
            text = "Stop"
            setOnClickListener { stopEverything() }
        }
        root.addView(statusView)
        root.addView(btnOverlay)
        root.addView(btnStart)
        root.addView(btnStop)
        setContentView(root)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        } else statusView.text = "Status: overlay granted"
    }

    private fun startCapture() {
        if (!Settings.canDrawOverlays(this)) {
            statusView.text = "Status: ขอสิทธิ์ overlay ก่อน"
            requestOverlayPermission(); return
        }
        addOverlay()
        statusView.text = "Status: รอ capture permission..."
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQ_PROJECTION)
    }

    private fun addOverlay() {
        if (overlayAdded) return
        overlayView = OverlayArrowView(this)
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        wm.addView(overlayView, lp)
        overlayAdded = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PROJECTION) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            statusView.text = "Status: capture ถูกปฏิเสธ"; return
        }
        startForegroundService(Intent(this, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_DATA, data)
        })
        ScreenCaptureService.listener = { frame -> processFrame(frame) }
        statusView.text = "Status: ทำงาน — เปิดเกมหมากรุก"
    }

    private fun processFrame(bitmap: android.graphics.Bitmap) {
        if (busy) return
        busy = true
        Thread {
            try {
                val board = BoardReader.readBoard(bitmap)
                val fen = Fen.fromBoard(board)
                if (fen != lastFen) {
                    lastFen = fen
                    val mv = engine.bestMove(fen, depth = 16, movetimeMs = 1200)
                    handler.post { overlayView?.setMove(mv) }
                }
            } catch (_: Throwable) {
            } finally { busy = false }
        }.start()
    }

    private fun stopEverything() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        ScreenCaptureService.listener = null
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView?.let { v -> if (overlayAdded) try { wm.removeView(v) } catch (_: Throwable) {} }
        overlayAdded = false; overlayView = null
        statusView.text = "Status: stopped"
    }

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
    }

    companion object { private const val REQ_PROJECTION = 1001 }
}
