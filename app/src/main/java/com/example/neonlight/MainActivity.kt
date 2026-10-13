package com.example.neonlight

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*

private val PINK = Color.parseColor("#FF2BD6")
private val CYAN = Color.parseColor("#00F0FF")

class TorchView(c: Context) : View(c) {
    var on = false
        set(v) { field = v; invalidate() }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 9f; strokeCap = Paint.Cap.ROUND }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = minOf(width, height) * 0.33f
        val color = if (on) CYAN else PINK
        if (on) {
            glow.color = Color.argb(90, 0, 240, 255)
            glow.maskFilter = BlurMaskFilter(r * 0.6f, BlurMaskFilter.Blur.NORMAL)
            cv.drawCircle(cx, cy, r * 1.25f, glow)
        }
        ring.color = color
        ring.maskFilter = BlurMaskFilter(if (on) 28f else 14f, BlurMaskFilter.Blur.SOLID)
        cv.drawCircle(cx, cy, r, ring)
        icon.color = color
        icon.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.SOLID)
        val u = r / 100f
        val head = Path().apply {
            moveTo(cx - 38 * u, cy - 62 * u); lineTo(cx + 38 * u, cy - 62 * u)
            lineTo(cx + 24 * u, cy - 28 * u); lineTo(cx - 24 * u, cy - 28 * u); close()
        }
        cv.drawPath(head, icon)
        cv.drawRoundRect(cx - 24 * u, cy - 28 * u, cx + 24 * u, cy + 66 * u, 12 * u, 12 * u, icon)
        cv.drawLine(cx, cy - 6 * u, cx, cy + 14 * u, icon)
        if (on) for (a in listOf(-35f, 0f, 35f)) {
            cv.save(); cv.rotate(a, cx, cy - 62 * u)
            cv.drawLine(cx, cy - 78 * u, cx, cy - 98 * u, icon); cv.restore()
        }
    }
}

class MainActivity : Activity() {
    private lateinit var cm: CameraManager
    private var camId: String? = null
    private var on = false
    private lateinit var torch: TorchView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0A0014")
        window.navigationBarColor = Color.parseColor("#0A0014")
        cm = getSystemService(CAMERA_SERVICE) as CameraManager
        camId = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0A0014"), Color.parseColor("#1B0636"), Color.parseColor("#05010D"))
            )
        }
        val title = TextView(this).apply {
            text = "NEON LIGHT"; textSize = 30f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(PINK); letterSpacing = 0.25f; gravity = Gravity.CENTER
            setShadowLayer(30f, 0f, 0f, PINK)
        }
        root.addView(title, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = 140 })

        torch = TorchView(this).apply { setOnClickListener { onTorchTap() } }
        root.addView(torch, FrameLayout.LayoutParams(-1, -1))

        status = TextView(this).apply {
            textSize = 16f; setTextColor(CYAN); gravity = Gravity.CENTER
            letterSpacing = 0.2f; setShadowLayer(20f, 0f, 0f, CYAN)
        }
        root.addView(status, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = 200 })
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        torch.on = on
        status.text = if (on) "ВКЛЮЧЕНО" else "НАЖМИ, ЧТОБЫ ВКЛЮЧИТЬ"
    }

    private fun setTorch(v: Boolean) {
        val id = camId
        if (id == null) { Toast.makeText(this, "Вспышка не найдена", Toast.LENGTH_SHORT).show(); return }
        try { cm.setTorchMode(id, v); on = v } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        refresh()
    }

    private fun onTorchTap() { if (!on) setTorch(true) else showPaywall() }

    private fun showPaywall() {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(60, 60, 60, 50)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#12002A")); cornerRadius = 48f; setStroke(5, PINK)
            }
        }
        fun tv(t: String, size: Float, col: Int, bold: Boolean = false) = TextView(this).apply {
            text = t; textSize = size; setTextColor(col); gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD; setShadowLayer(18f, 0f, 0f, col)
            setPadding(0, 12, 0, 12)
        }
        fun btn(t: String, col: Int, action: () -> Unit) = Button(this).apply {
            text = t; setTextColor(col); isAllCaps = false
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT); cornerRadius = 40f; setStroke(4, col) }
            setOnClickListener { d.dismiss(); action() }
        }
        box.addView(tv("🔒 Премиум-функция", 22f, PINK, true))
        box.addView(tv("Купить функцию выключения", 17f, Color.WHITE))
        box.addView(tv("\$399", 44f, CYAN, true))
        box.addView(btn("Купить", CYAN) {
            Toast.makeText(this, "Шутка! Платить не нужно 😄", Toast.LENGTH_LONG).show()
            setTorch(false)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 30 })
        box.addView(btn("Не сейчас", PINK) {}, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })
        d.setContentView(box)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.window?.setLayout((resources.displayMetrics.widthPixels * 0.88).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        d.show()
    }
}
