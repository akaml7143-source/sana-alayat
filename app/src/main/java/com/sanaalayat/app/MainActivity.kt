package com.sanaalayat.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val maroon = Color.rgb(112, 22, 38)
    private val gold = Color.rgb(177, 137, 67)
    private val ink = Color.rgb(48, 42, 37)
    private val paper = Color.rgb(250, 247, 239)

    private lateinit var pages: JSONArray
    private lateinit var versePages: JSONArray
    private lateinit var legend: JSONArray
    private lateinit var originalLines: List<String>
    private lateinit var pageContent: LinearLayout
    private lateinit var pageScroll: ScrollView
    private lateinit var pageLabel: TextView
    private lateinit var pageInput: EditText
    private lateinit var searchInput: EditText
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = maroon
        window.navigationBarColor = Color.rgb(68, 12, 24)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        try {
            val data = assets.open("quran-pages.json").bufferedReader(Charsets.UTF_8).use {
                JSONObject(it.readText())
            }
            pages = data.getJSONArray("pages")
            versePages = data.getJSONArray("versePages")
            legend = data.getJSONArray("legend")
            originalLines = assets.open("quran-uthmani-kitab.txt").bufferedReader(Charsets.UTF_8).use {
                it.readLines()
            }
            if (pages.length() != 604 || originalLines.size != 6236 || versePages.length() != 6236) {
                throw IllegalStateException("بيانات صفحات المصحف غير مكتملة")
            }
            currentPage = getPreferences(MODE_PRIVATE).getInt("reading_page", 1).coerceIn(1, 604)
            buildScreen()
            renderPage()
        } catch (error: Exception) {
            showLoadError(error)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(paper)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundColor(maroon)
        }
        header.addView(TextView(this).apply {
            text = "سَنَا الآيَات"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        header.addView(TextView(this).apply {
            text = "مُصْحَفُ المَدِينَةِ النَّبَوِيَّةِ • رِوَايَةُ حَفْصٍ"
            textSize = 13f
            setTextColor(Color.rgb(244, 224, 185))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(header)

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(4))
        }
        searchInput = EditText(this).apply {
            hint = "ابحث عن كلمة أو عبارة"
            textSize = 14f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(ink)
            setHintTextColor(Color.GRAY)
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedBackground(Color.WHITE, gold)
        }
        searchRow.addView(searchInput, LinearLayout.LayoutParams(0, dp(44), 1f))
        searchRow.addView(makeButton("بحث") { searchQuran() }, LinearLayout.LayoutParams(dp(70), dp(44)).apply {
            marginStart = dp(7)
        })
        root.addView(searchRow)

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(3), dp(10), dp(6))
        }
        navigation.addView(makeButton("السابق") { changePage(-1) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        pageLabel = TextView(this).apply {
            text = "الصفحة 1 من 604"
            textSize = 15f
            setTextColor(maroon)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        navigation.addView(pageLabel, LinearLayout.LayoutParams(0, dp(42), 1.4f))
        navigation.addView(makeButton("التالي") { changePage(1) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        root.addView(navigation)

        val jumpRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), dp(8))
        }
        pageInput = EditText(this).apply {
            hint = "رقم الصفحة"
            textSize = 14f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            setTextColor(ink)
            setPadding(dp(6), 0, dp(6), 0)
            background = roundedBackground(Color.WHITE, gold)
        }
        jumpRow.addView(pageInput, LinearLayout.LayoutParams(dp(105), dp(40)))
        jumpRow.addView(makeButton("انتقال") { jumpToPage() }, LinearLayout.LayoutParams(dp(82), dp(40)).apply {
            marginStart = dp(7)
        })
        jumpRow.addView(makeButton("دليل الألوان") { showLegend() }, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
            marginStart = dp(7)
        })
        root.addView(jumpRow)

        pageScroll = ScrollView(this).apply {
            setFillViewport(true)
            isVerticalScrollBarEnabled = true
            setPadding(dp(10), dp(2), dp(10), dp(8))
        }
        pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = roundedBackground(Color.rgb(255, 253, 247), gold)
            setPadding(dp(10), dp(12), dp(10), dp(14))
        }
        pageScroll.addView(pageContent)
        root.addView(pageScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "اللَّهُمَّ اجْعَلِ الْقُرْآنَ رَبِيعَ قُلُوبِنَا"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(maroon)
            setPadding(dp(8), dp(7), dp(8), dp(8))
        })
        setContentView(root)
    }

    private fun renderPage() {
        val page = pages.getJSONObject(currentPage - 1)
        val lines = page.getJSONArray("lines")
        pageContent.removeAllViews()
        pageLabel.text = "الصفحة ${arabicDigits(currentPage)} من ٦٠٤"
        for (index in 0 until lines.length()) {
            val line = lines.getJSONObject(index)
            when (line.getString("type")) {
                "header" -> addPageText(line.optString("text"), 17f, maroon, true, dp(7))
                "basmala" -> addPageText(line.optString("text"), 18f, maroon, false, dp(5))
                "ayah-line" -> addAyahLine(line.getJSONArray("words"))
            }
        }
        pageScroll.post { pageScroll.scrollTo(0, 0) }
        getPreferences(MODE_PRIVATE).edit().putInt("reading_page", currentPage).apply()
    }

    private fun addPageText(text: String, size: Float, color: Int, bold: Boolean, verticalPadding: Int) {
        val view = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface = Typeface.create("serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(2), verticalPadding, dp(2), verticalPadding)
        }
        pageContent.addView(view, LinearLayout.LayoutParams(-1, -2))
    }

    private fun addAyahLine(words: JSONArray) {
        val builder = SpannableStringBuilder()
        for (i in 0 until words.length()) {
            val word = words.getJSONObject(i)
            val text = word.optString("text")
            val start = builder.length
            builder.append(text)
            val end = builder.length
            val colorHex = word.optString("color")
            if (colorHex.isNotBlank()) {
                try {
                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor(colorHex)),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } catch (_: IllegalArgumentException) {
                    // Keep the preserved Quran text in its default ink color.
                }
            }
            if (word.optBoolean("endVerse")) {
                builder.append(" ﴿")
                builder.append(arabicDigits(word.optInt("ayah")))
                builder.append("﴾")
            }
            if (i < words.length() - 1) builder.append("  ")
        }
        val view = TextView(this).apply {
            this.text = builder
            textSize = 17f
            setTextColor(ink)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            isSingleLine = true
            setPadding(0, dp(5), 0, dp(5))
        }
        pageContent.addView(view, LinearLayout.LayoutParams(-1, -2))
    }

    private fun changePage(delta: Int) {
        val target = (currentPage + delta).coerceIn(1, 604)
        if (target == currentPage) return
        currentPage = target
        renderPage()
    }

    private fun jumpToPage() {
        val target = pageInput.text.toString().toIntOrNull()
        if (target == null || target !in 1..604) {
            Toast.makeText(this, "أدخل رقم صفحة من 1 إلى 604", Toast.LENGTH_SHORT).show()
            return
        }
        currentPage = target
        pageInput.setText("")
        renderPage()
    }

    private fun searchQuran() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "اكتب كلمة للبحث أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        val index = originalLines.indexOfFirst { it.contains(query, ignoreCase = false) }
        if (index < 0) {
            Toast.makeText(this, "لم يتم العثور على العبارة", Toast.LENGTH_SHORT).show()
            return
        }
        currentPage = versePages.optInt(index, 1).coerceIn(1, 604)
        renderPage()
        Toast.makeText(this, "تم العثور على العبارة في الصفحة ${arabicDigits(currentPage)}", Toast.LENGTH_LONG).show()
    }

    private fun showLegend() {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        for (i in 0 until legend.length()) {
            val item = legend.getJSONObject(i)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }
            row.addView(TextView(this).apply {
                text = "●"
                textSize = 21f
                setTextColor(Color.parseColor(item.optString("color", "#333333")))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(34), dp(32)))
            val description = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            description.addView(TextView(this).apply {
                text = item.optString("name")
                textSize = 15f
                setTextColor(ink)
                typeface = Typeface.DEFAULT_BOLD
            })
            val note = item.optString("description")
            if (note.isNotBlank()) {
                description.addView(TextView(this).apply {
                    text = note
                    textSize = 11f
                    setTextColor(Color.DKGRAY)
                })
            }
            row.addView(description, LinearLayout.LayoutParams(0, -2, 1f))
            list.addView(row)
        }
        val scroll = ScrollView(this).apply { addView(list) }
        AlertDialog.Builder(this)
            .setTitle("دليل ألوان أحكام التجويد")
            .setView(scroll)
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun showLoadError(error: Exception) {
        val message = "تعذّر تحميل صفحات المصحف أو بيانات التجويد.\n${error.message ?: ""}"
        setContentView(TextView(this).apply {
            text = message
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(maroon)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        })
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setTextColor(maroon)
            background = roundedBackground(Color.WHITE, gold)
            setOnClickListener { action() }
        }
    }

    private fun roundedBackground(color: Int, stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun arabicDigits(number: Int): String =
        number.toString().map { digit ->
            if (digit in '0'..'9') ('٠'.code + (digit - '0')).toChar() else digit
        }.joinToString("")

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        getPreferences(MODE_PRIVATE).edit().putInt("reading_page", currentPage).apply()
        super.onPause()
    }
}
