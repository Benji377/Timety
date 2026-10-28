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
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import io.github.benji377.timety.R
import java.io.ByteArrayOutputStream
import java.text.NumberFormat

/** Draws the shareable wrap-up card: the user's lifetime totals on a 1080x1920 story image. */
object WrapUpImageGenerator {
    private const val W = 1080f
    private const val H = 1920f
    private const val PAD = 96f
    private const val LOGO_SIZE = 140
    private const val ICON_BOX_SIZE = 132f
    private const val ICON_INSET = 30
    private const val STAT_ROW_GAP = 72f
    private const val FOOTER_BASELINE = H - 140f

    // Colors matching the app's theme palette.
    private val PAPER_DARK = "#151515".toColorInt()
    private val GRADIENT_MID = "#2A2418".toColorInt()
    private val TASK_COLOR = "#2563EB".toColorInt()
    private val TASK_COLOR_90 = "#E62563EB".toColorInt()
    private val SUCCESS_COLOR = "#16A34A".toColorInt()
    private val HABIT_COLOR = "#7C3AED".toColorInt()
    private val WHITE_70 = "#B3FFFFFF".toColorInt()
    private val WHITE_55 = "#8CFFFFFF".toColorInt()

    /** The card for [name]'s totals; [since] is the formatted join date, left out when null. */
    fun render(
        context: Context,
        name: String,
        since: String?,
        tasksCompleted: Int,
        focusMins: Int,
        habitsMet: Int,
    ): Bitmap {
        val bitmap = createBitmap(W.toInt(), H.toInt())
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        var y = drawLogo(context, canvas, startY = 140f)
        y = drawHeader(context, canvas, name, since, y)
        drawStats(
            canvas,
            listOf(
                StatRow(R.drawable.ic_wrapup_tasks, TASK_COLOR, tasksCompleted, context.getString(R.string.wrapUpStatTasksLabel)),
                StatRow(R.drawable.ic_wrapup_focus, SUCCESS_COLOR, focusMins, context.getString(R.string.wrapUpStatFocusLabel)),
                StatRow(R.drawable.ic_wrapup_habits, HABIT_COLOR, habitsMet, context.getString(R.string.wrapUpStatHabitsLabel)),
            ),
            context,
            headerBottom = y,
        )
        drawFooter(context, canvas)
        return bitmap
    }

    fun toPng(bitmap: Bitmap): ByteArray {
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
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
        drawable?.setBounds(PAD.toInt(), startY.toInt(), PAD.toInt() + LOGO_SIZE, startY.toInt() + LOGO_SIZE)
        drawable?.draw(canvas)
        return startY + LOGO_SIZE + 48f
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        name: String,
        since: String?,
        startY: Float,
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TASK_COLOR
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        var y = paintTextTopLeft(canvas, context.getString(R.string.wrapUpHeaderEyebrow), PAD, startY, paint)
        y += 24f

        paint.apply {
            color = Color.WHITE
            textSize = 96f
            letterSpacing = 0f
        }
        y = paintFittedText(canvas, context.getString(R.string.wrapUpHeaderTitle, name), PAD, y, paint)

        if (since != null) {
            y += 12f
            val sincePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = WHITE_70
                textSize = 40f
            }
            y = paintFittedText(canvas, context.getString(R.string.wrapUpUsingSince, since), PAD, y, sincePaint)
        }
        return y
    }

    private fun drawStats(canvas: Canvas, rows: List<StatRow>, context: Context, headerBottom: Float) {
        // Centered in the space between the header and the footer.
        val blockHeight = rows.size * ICON_BOX_SIZE + (rows.size - 1) * STAT_ROW_GAP
        var y = headerBottom + (FOOTER_BASELINE - 80f - headerBottom - blockHeight) / 2
        rows.forEach { row ->
            drawStatRow(context, canvas, row, y)
            y += ICON_BOX_SIZE + STAT_ROW_GAP
        }
    }

    private fun drawStatRow(context: Context, canvas: Canvas, row: StatRow, y: Float) {
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = row.color
            alpha = (255 * 0.22).toInt()
        }
        canvas.drawRoundRect(RectF(PAD, y, PAD + ICON_BOX_SIZE, y + ICON_BOX_SIZE), 28f, 28f, boxPaint)

        ContextCompat.getDrawable(context, row.icon)?.mutate()?.apply {
            setTint(row.color)
            setBounds(
                PAD.toInt() + ICON_INSET,
                y.toInt() + ICON_INSET,
                (PAD + ICON_BOX_SIZE).toInt() - ICON_INSET,
                (y + ICON_BOX_SIZE).toInt() - ICON_INSET,
            )
            draw(canvas)
        }

        val textX = PAD + ICON_BOX_SIZE + 36f
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE_70
            textSize = 36f
        }
        // Value and label sit as one block centered on the icon box.
        val valueHeight = valuePaint.fontMetrics.let { it.bottom - it.top }
        val labelHeight = labelPaint.fontMetrics.let { it.bottom - it.top }
        val top = y + (ICON_BOX_SIZE - valueHeight - labelHeight) / 2
        val labelTop = paintTextTopLeft(canvas, NumberFormat.getIntegerInstance().format(row.value), textX, top, valuePaint)
        paintTextTopLeft(canvas, row.label, textX, labelTop, labelPaint)
    }

    private fun drawFooter(context: Context, canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE_55
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.wrapUpFooter), W / 2f, FOOTER_BASELINE, paint)
    }

    /** Draws [text] with its top at [y] and returns the y just below it. */
    private fun paintTextTopLeft(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint): Float {
        canvas.drawText(text, x, y - paint.fontMetrics.top, paint)
        return y + (paint.fontMetrics.bottom - paint.fontMetrics.top)
    }

    /** Like [paintTextTopLeft], shrinking the text until it fits the card's width. */
    private fun paintFittedText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint): Float {
        while (paint.measureText(text) > W - PAD * 2 && paint.textSize > 20f) {
            paint.textSize -= 4f
        }
        return paintTextTopLeft(canvas, text, x, y, paint)
    }

    private data class StatRow(
        @param:DrawableRes val icon: Int,
        val color: Int,
        val value: Int,
        val label: String,
    )
}
