package com.example.watranslator

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var button: TextView? = null
    private var panel: View? = null

    private fun client(s: String, t: String) = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage(s).setTargetLanguage(t).build()
    )
    private val arToEn by lazy { client(TranslateLanguage.ARABIC, TranslateLanguage.ENGLISH) }
    private val enToAr by lazy { client(TranslateLanguage.ENGLISH, TranslateLanguage.ARABIC) }
    private val arabic = Regex("[\u0600-\u06FF]")
    private val timeOnly = Regex("^\\d{1,2}:\\d{2}.*")
    private val whatsapp = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val c = DownloadConditions.Builder().build()
        arToEn.downloadModelIfNeeded(c)
        enToAr.downloadModelIfNeeded(c)
        addButton()
    }

    private fun overlayParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    private fun addButton() {
        val b = TextView(this).apply {
            text = "ترجم"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(36, 24, 36, 24)
            background = GradientDrawable().apply { cornerRadius = 80f; setColor(0xFF128C5A.toInt()) }
            setOnClickListener { translateScreen() }
        }
        val lp = overlayParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        wm.addView(b, lp)
        button = b
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val p = e.packageName?.toString() ?: return
        if (p in whatsapp) {
            button?.visibility = View.VISIBLE
        } else if (p != packageName && p != "com.android.systemui" &&
            !p.contains("inputmethod") && !p.contains("keyboard")
        ) {
            button?.visibility = View.GONE
            closePanel()
        }
    }

    private fun translateScreen() {
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString()
        if (pkg == null || pkg !in whatsapp) return
        val byId = mutableListOf<String>()
        val any = mutableListOf<String>()
        walk(root, byId, any)
        showPanel((if (byId.isNotEmpty()) byId else any).takeLast(8))
    }

    private fun walk(n: AccessibilityNodeInfo?, byId: MutableList<String>, any: MutableList<String>) {
        if (n == null) return
        val t = n.text?.toString()?.trim()
        if (!t.isNullOrEmpty() && n.className == "android.widget.TextView") {
            if (n.viewIdResourceName?.endsWith("message_text") == true) byId.add(t)
            else if (t.length > 1 && !timeOnly.matches(t)) any.add(t)
        }
        for (i in 0 until n.childCount) walk(n.getChild(i), byId, any)
    }

    private fun row(s: String) = TextView(this).apply {
        text = s
        textSize = 16f
        setTextColor(Color.BLACK)
        setPadding(0, 14, 0, 14)
        textDirection = View.TEXT_DIRECTION_ANY_RTL
    }

    private fun showPanel(texts: List<String>) {
        closePanel()
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 32) }
        val scroll = ScrollView(this).apply { addView(list) }
        val close = TextView(this).apply {
            text = "✕ إغلاق"
            setTextColor(Color.GRAY)
            setPadding(32, 24, 32, 8)
            setOnClickListener { closePanel() }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(40f, 40f, 40f, 40f, 0f, 0f, 0f, 0f)
                setColor(Color.WHITE)
            }
            addView(close)
            addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.45).toInt()))
        }
        wm.addView(box, overlayParams(MATCH_PARENT, WRAP_CONTENT).apply { gravity = Gravity.BOTTOM })
        panel = box

        if (texts.isEmpty()) {
            list.addView(row("مفيش رسايل ظاهرة على الشاشة"))
            return
        }
        texts.forEach { t ->
            val tv = row("…")
            list.addView(tv)
            val c = if (arabic.containsMatchIn(t)) arToEn else enToAr
            c.translate(t)
                .addOnSuccessListener { tv.text = it }
                .addOnFailureListener { tv.text = "الترجمة مش جاهزة. اتأكد من النت أول مرة عشان تتحمل اللغات." }
        }
    }

    private fun closePanel() {
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        closePanel()
        button?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }
}
