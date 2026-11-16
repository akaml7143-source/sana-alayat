package com.sanaalayat.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val maroon = Color.rgb(125, 20, 35)
    private val gold = Color.rgb(183, 145, 75)
    private val ink = Color.rgb(45, 38, 34)
    private val paper = Color.rgb(250, 247, 240)
    private lateinit var content: TextView
    private lateinit var scroll: ScrollView
    private lateinit var allText: String
    private lateinit var search: EditText
    private var savedY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = maroon
        window.navigationBarColor = Color.rgb(75, 12, 22)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        savedY = getPreferences(MODE_PRIVATE).getInt("reading_position", 0)
        allText = try {
            assets.open("quran-uthmani-kitab.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            "تعذّر فتح ملف القرآن. يرجى التحقق من وجود quran-uthmani-kitab.txt ضمن ملفات التطبيق."
        }
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(paper)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 22, 20, 18)
            setBackgroundColor(maroon)
        }
        header.addView(TextView(this).apply {
            text = "سَنَا الآيَات"
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        header.addView(TextView(this).apply {
            text = "وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا"
            textSize = 16f
            setTextColor(Color.rgb(245, 224, 184))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        })
        root.addView(header)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 8)
            gravity = Gravity.CENTER_VERTICAL
        }
        search = EditText(this).apply {
            hint = "ابحث في نص القرآن"
            textSize = 15f
            singleLine = true
            setTextColor(ink)
            setHintTextColor(Color.GRAY)
            setPadding(14, 8, 14, 8)
            background = roundedBackground(Color.WHITE, gold)
        }
        tools.addView(search, LinearLayout.LayoutParams(0, 48, 1f))
        val find = Button(this).apply {
            text = "بحث"
            setTextColor(Color.WHITE)
            background = roundedBackground(maroon, maroon)
            isAllCaps = false
            setOnClickListener { showSearchResults(search.text.toString()) }
        }
        tools.addView(find, LinearLayout.LayoutParams(76, 48).apply { marginStart = 8 })
        root.addView(tools)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(12, 0, 12, 10)
        }
        val all = Button(this).apply {
            text = "عرض القرآن كاملاً"
            isAllCaps = false
            setTextColor(maroon)
            setOnClickListener { showAllText(); search.setText("") }
        }
        val resume = Button(this).apply {
            text = "متابعة القراءة"
            isAllCaps = false
            setTextColor(maroon)
            setOnClickListener { scroll.post { scroll.scrollTo(0, savedY) } }
        }
        actions.addView(all, LinearLayout.LayoutParams(0, 46, 1f))
        actions.addView(resume, LinearLayout.LayoutParams(0, 46, 1f))
        root.addView(actions)

        scroll = ScrollView(this).apply {
            fillViewport = true
            isVerticalScrollBarEnabled = true
            setPadding(14, 4, 14, 18)
        }
        content = TextView(this).apply {
            textSize = 23f
            setTextColor(ink)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.RIGHT
            textDirection = View.TEXT_DIRECTION_RTL
            setLineSpacing(12f, 1.25f)
            setTextIsSelectable(true)
            setPadding(10, 18, 10, 26)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = TextView(this).apply {
            text = "اللَّهُمَّ اجْعَلِ الْقُرْآنَ رَبِيعَ قُلُوبِنَا"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(maroon)
            setPadding(8, 12, 8, 14)
        }
        root.addView(footer)
        setContentView(root)
        showAllText()
        scroll.post { scroll.scrollTo(0, savedY) }
        scroll.setOnScrollChangeListener { _, _, y, _, _ ->
            savedY = y
            getPreferences(MODE_PRIVATE).edit().putInt("reading_position", y).apply()
        }
    }

    private fun showAllText() {
        content.text = allText
    }

    private fun showSearchResults(query: String) {
        val term = query.trim()
        if (term.isEmpty()) {
            Toast.makeText(this, "اكتب كلمة للبحث أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        val matches = allText.lineSequence().filter { it.contains(term, ignoreCase = false) }.take(500).toList()
        content.text = if (matches.isEmpty()) {
            "لم يتم العثور على نتائج لعبارة: $term"
        } else {
            "نتائج البحث عن «$term»\n\n" + matches.joinToString("\n\n")
        }
        scroll.scrollTo(0, 0)
    }

    private fun roundedBackground(color: Int, stroke: Int): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = 16f
            setStroke(1, stroke)
        }
    }

    override fun onPause() {
        getPreferences(MODE_PRIVATE).edit().putInt("reading_position", savedY).apply()
        super.onPause()
    }
}
