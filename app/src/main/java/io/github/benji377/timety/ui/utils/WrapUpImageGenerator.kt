package io.github.benji377.timety.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import io.github.benji377.timety.R
import java.io.ByteArrayOutputStream

/** Renders the shareable "year/period wrap-up" summary card as a PNG, drawn directly on a Canvas. */
object WrapUpImageGenerator {
    private const val W = 1080f
    private const val H = 1920f
    private const val PAD = 88f
    private const val ICON_BOX_SIZE = 100f
    private const val STAT_ROW_HEIGHT = 108f
    private const val STAT_ROW_GAP = 52f

    // Colors matching the app's theme palette.
    private val PAPER_DARK = "#151515".toColorInt()
    private val GRADIENT_MID = "#2A2418".toColorInt()
    private val TASK_COLOR = "#2563EB".toColorInt()
    private val TASK_COLOR_90 = "#E62563EB".toColorInt()
    private val WARNING_COLOR = "#F59E0B".toColorInt()
    private val SUCCESS_COLOR = "#16A34A".toColorInt()
    private val HABIT_COLOR = "#7C3AED".toColorInt()

    /** Draws the wrap-up card for the given stats and returns it encoded as a PNG byte array. */
    fun generate(
        context: Context,
        name: String,
        level: Int,
        levelTitle: String,
        streak: Int,
        tasksCompleted: Int,
        focusMins: Int,
        habitsMet: Int
    ): ByteArray {
        val bitmap = createBitmap(W.toInt(), H.toInt())
        val canvas = Canvas(bitmap)

        canvas.scale(1.2f, 1.2f)
        canvas.translate(0f, 50f)

        drawBackground(canvas)

        var y = H * 0.085f
        y = drawLogo(context, canvas, y)
        y = drawHeader(context, canvas, name, level, levelTitle, y)
        drawStats(context, canvas, streak, tasksCompleted, focusMins, habitsMet, y)
        drawFooter(context, canvas)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, W, H,
                intArrayOf(PAPER_DARK, GRADIENT_MID, TASK_COLOR_90),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, W, H, paint)
    }

    private fun drawLogo(context: Context, canvas: Canvas, startY: Float): Float {
        val size = 112
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)

        if (drawable != null) {
            drawable.setBounds(
                PAD.toInt(),
                startY.toInt(),
                (PAD + size).toInt(),
                (startY + size).toInt()
            )
            drawable.draw(canvas)
        }
        return startY + size + 36f
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        name: String,
        level: Int,
        levelTitle: String,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TASK_COLOR
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        y = paintTextTopLeft(canvas, context.getString(R.string.wrapUpHeaderEyebrow), PAD, y, paint)
        y += 52f

        paint.apply {
            color = Color.WHITE
            textSize = 82f
            letterSpacing = 0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        y = paintFittedText(
            canvas,
            context.getString(R.string.wrapUpHeaderTitle, name),
            PAD,
            y,
            paint,
            W - PAD * 2
        )
        y += 14f

        paint.apply {
            color = WARNING_COLOR
            textSize = 38f
        }
        y = paintTextTopLeft(
            canvas,
            context.getString(R.string.wrapUpHeaderLevel, level, levelTitle),
            PAD,
            y,
            paint
        )
        y += 58f

        return y
    }

    private fun drawStats(
        context: Context,
        canvas: Canvas,
        streak: Int,
        tasks: Int,
        focus: Int,
        habits: Int,
        headerBottom: Float
    ) {
        val startY = headerBottom + 100f
        var y = startY

        val rows = listOf(
            StatRow(
                "🔥", WARNING_COLOR,
                context.getString(R.string.wrapUpStatStreakValue, streak),
                context.getString(R.string.wrapUpStatStreakLabel)
            ),
            StatRow("✅", TASK_COLOR, "$tasks", context.getString(R.string.wrapUpStatTasksLabel)),
            StatRow(
                "⏱️",
                SUCCESS_COLOR,
                "$focus",
                context.getString(R.string.wrapUpStatFocusLabel)
            ),
            StatRow("🔁", HABIT_COLOR, "$habits", context.getString(R.string.wrapUpStatHabitsLabel))
        )

        for (row in rows) {
            drawStatRow(canvas, row, y)
            y += STAT_ROW_HEIGHT + STAT_ROW_GAP
        }
    }

    private fun drawStatRow(canvas: Canvas, row: StatRow, y: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = row.color
            alpha = (255 * 0.22).toInt()
        }
        canvas.drawRoundRect(
            RectF(PAD, y, PAD + ICON_BOX_SIZE, y + ICON_BOX_SIZE),
            22f, 22f, bgPaint
        )

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 54f
            textAlign = Paint.Align.CENTER
        }
        val textY = y + (ICON_BOX_SIZE / 2) - ((emojiPaint.descent() + emojiPaint.ascent()) / 2)
        canvas.drawText(row.icon, PAD + ICON_BOX_SIZE / 2, textY, emojiPaint)

        val textX = PAD + ICON_BOX_SIZE + 30f

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        paintTextTopLeft(canvas, row.value, textX, y + 2f, valuePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#B3FFFFFF".toColorInt()
            textSize = 30f
        }
        paintTextTopLeft(canvas, row.label, textX, y + 58f, labelPaint)
    }

    private fun drawFooter(context: Context, canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#8CFFFFFF".toColorInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val y = H - 550f - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(context.getString(R.string.wrapUpFooter), W / 2f, y, paint)
    }

    private fun paintTextTopLeft(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ): Float {
        val baselineY = y - paint.fontMetrics.ascent
        canvas.drawText(text, x, baselineY, paint)
        return y + (paint.fontMetrics.bottom - paint.fontMetrics.top)
    }

    private fun paintFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float
    ): Float {
        var currentSize = paint.textSize
        while (paint.measureText(text) > maxWidth && currentSize >= 20f) {
            currentSize -= 4f
            paint.textSize = currentSize
        }
        return paintTextTopLeft(canvas, text, x, y, paint)
    }

    private data class StatRow(
        val icon: String,
        val color: Int,
        val value: String,
        val label: String
    )
}
