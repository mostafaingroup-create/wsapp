package com.example.watranslator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = TextView(this).apply {
            text = "1) دوس على الزرار تحت.\n2) من \"التطبيقات المثبتة\" اختار \"مترجم الواتساب\" وفعّله.\n3) افتح الواتساب، هيظهر زر \"ترجم\" على الجنب.\n4) دوس عليه وهتتترجم الرسايل الظاهرة قدامك.\n\nأول مرة لازم نت عشان تتحمل لغات الترجمة، بعد كده بتشتغل من غير نت."
            textSize = 17f
            textDirection = View.TEXT_DIRECTION_ANY_RTL
        }
        val open = Button(this).apply {
            text = "فتح إعدادات إمكانية الوصول"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(info)
            addView(open)
        })
    }
}
