package com.backlogzero.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Eenvoudig scherm zonder Compose dat de foutmelding toont als de app crasht. Draait in een eigen
 * proces, dus het werkt ook als de crash al bij het opstarten gebeurt.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra("t") ?: "Geen details beschikbaar."
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            this.text = "Backlog Zero is gecrasht"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(pad, pad * 2, pad, pad / 2)
        }
        val hint = TextView(this).apply {
            this.text = "Maak een foto van dit scherm of kopieer de tekst en stuur die door."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(pad, 0, pad, pad / 2)
        }
        val copy = Button(this).apply {
            this.text = "Kopieer foutmelding"
            setOnClickListener {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("crash", text))
            }
        }
        val body = TextView(this).apply {
            this.text = text
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setTextIsSelectable(true)
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(this).apply { addView(body) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0E12"))
            addView(title)
            addView(hint)
            addView(copy)
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
    }
}
