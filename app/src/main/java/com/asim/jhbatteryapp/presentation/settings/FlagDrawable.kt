package com.asim.jhbatteryapp.presentation.settings

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.cos
import kotlin.math.sin

/**
 * Programmatic flag drawable rendered dynamically via Canvas code.
 * Supports all requested app languages:
 * ar (Saudi Arabia), de (Germany), as (India/Assam), fa (Iran), fr (France),
 * hi (India), in/id (Indonesia), iw/he (Israel), ja (Japan), ko (South Korea),
 * ms (Malaysia), pt (Portugal), ru (Russia), tr (Turkey), ur (Pakistan),
 * vi (Vietnam), zh (China), en (USA/UK), nl (Netherlands).
 */
class FlagDrawable(private val langCode: String) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#1F000000") // subtle 12% black border
    }
    private val clipPath = Path()
    private val shapePath = Path()
    private val rectF = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val left = b.left.toFloat()
        val top = b.top.toFloat()
        val right = b.right.toFloat()
        val bottom = b.bottom.toFloat()

        rectF.set(left, top, right, bottom)
        val cornerRadius = h * 0.16f

        clipPath.reset()
        clipPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)

        val saveCount = canvas.save()
        canvas.clipPath(clipPath)

        when (langCode.lowercase()) {
            "ar" -> drawSaudiArabia(canvas, left, top, w, h)
            "de" -> drawGermany(canvas, left, top, w, h)
            "as", "hi" -> drawIndia(canvas, left, top, w, h)
            "fa" -> drawIran(canvas, left, top, w, h)
            "fr" -> drawFrance(canvas, left, top, w, h)
            "in", "id" -> drawIndonesia(canvas, left, top, w, h)
            "iw", "he" -> drawIsrael(canvas, left, top, w, h)
            "ja" -> drawJapan(canvas, left, top, w, h)
            "ko" -> drawSouthKorea(canvas, left, top, w, h)
            "ms" -> drawMalaysia(canvas, left, top, w, h)
            "pt" -> drawPortugal(canvas, left, top, w, h)
            "ru" -> drawRussia(canvas, left, top, w, h)
            "tr" -> drawTurkey(canvas, left, top, w, h)
            "ur" -> drawPakistan(canvas, left, top, w, h)
            "vi" -> drawVietnam(canvas, left, top, w, h)
            "zh" -> drawChina(canvas, left, top, w, h)
            "nl" -> drawNetherlands(canvas, left, top, w, h)
            "en" -> drawUSA(canvas, left, top, w, h)
            else -> drawUSA(canvas, left, top, w, h)
        }

        // Draw crisp border
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)
        canvas.restoreToCount(saveCount)
    }

    private fun drawGermany(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val h3 = h / 3f
        paint.color = Color.parseColor("#000000")
        canvas.drawRect(l, t, l + w, t + h3, paint)
        paint.color = Color.parseColor("#DD0000")
        canvas.drawRect(l, t + h3, l + w, t + h3 * 2, paint)
        paint.color = Color.parseColor("#FFCE00")
        canvas.drawRect(l, t + h3 * 2, l + w, t + h, paint)
    }

    private fun drawFrance(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val w3 = w / 3f
        paint.color = Color.parseColor("#002654")
        canvas.drawRect(l, t, l + w3, t + h, paint)
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l + w3, t, l + w3 * 2, t + h, paint)
        paint.color = Color.parseColor("#ED2939")
        canvas.drawRect(l + w3 * 2, t, l + w, t + h, paint)
    }

    private fun drawIndia(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val h3 = h / 3f
        // Saffron
        paint.color = Color.parseColor("#FF9933")
        canvas.drawRect(l, t, l + w, t + h3, paint)
        // White
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l, t + h3, l + w, t + h3 * 2, paint)
        // Green
        paint.color = Color.parseColor("#128807")
        canvas.drawRect(l, t + h3 * 2, l + w, t + h, paint)

        // Ashoka Chakra (navy blue circle + center dot + spokes)
        val cx = l + w / 2f
        val cy = t + h / 2f
        val r = h3 * 0.42f
        val chakraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#000080")
            style = Paint.Style.STROKE
            strokeWidth = h * 0.04f
        }
        canvas.drawCircle(cx, cy, r, chakraPaint)
        chakraPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, r * 0.22f, chakraPaint)

        chakraPaint.strokeWidth = h * 0.025f
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val ex = cx + (r * cos(angle)).toFloat()
            val ey = cy + (r * sin(angle)).toFloat()
            val sx = cx - (r * cos(angle)).toFloat()
            val sy = cy - (r * sin(angle)).toFloat()
            canvas.drawLine(sx, sy, ex, ey, chakraPaint)
        }
    }

    private fun drawIndonesia(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val h2 = h / 2f
        paint.color = Color.parseColor("#ED2939")
        canvas.drawRect(l, t, l + w, t + h2, paint)
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l, t + h2, l + w, t + h, paint)
    }

    private fun drawRussia(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val h3 = h / 3f
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l, t, l + w, t + h3, paint)
        paint.color = Color.parseColor("#0039A6")
        canvas.drawRect(l, t + h3, l + w, t + h3 * 2, paint)
        paint.color = Color.parseColor("#D52B1E")
        canvas.drawRect(l, t + h3 * 2, l + w, t + h, paint)
    }

    private fun drawNetherlands(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val h3 = h / 3f
        paint.color = Color.parseColor("#AE1C28")
        canvas.drawRect(l, t, l + w, t + h3, paint)
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l, t + h3, l + w, t + h3 * 2, paint)
        paint.color = Color.parseColor("#21468B")
        canvas.drawRect(l, t + h3 * 2, l + w, t + h, paint)
    }

    private fun drawJapan(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l, t, l + w, t + h, paint)
        paint.color = Color.parseColor("#BC002D")
        canvas.drawCircle(l + w / 2f, t + h / 2f, h * 0.28f, paint)
    }

    private fun drawVietnam(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.parseColor("#DA251D")
        canvas.drawRect(l, t, l + w, t + h, paint)
        paint.color = Color.parseColor("#FFFF00")
        drawStar(canvas, l + w / 2f, t + h / 2f, h * 0.32f, paint)
    }

    private fun drawChina(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.parseColor("#DE2910")
        canvas.drawRect(l, t, l + w, t + h, paint)
        paint.color = Color.parseColor("#FFDE00")
        // Big star
        drawStar(canvas, l + w * 0.24f, t + h * 0.35f, h * 0.22f, paint)
        // 4 smaller stars
        drawStar(canvas, l + w * 0.44f, t + h * 0.18f, h * 0.075f, paint)
        drawStar(canvas, l + w * 0.50f, t + h * 0.30f, h * 0.075f, paint)
        drawStar(canvas, l + w * 0.50f, t + h * 0.46f, h * 0.075f, paint)
        drawStar(canvas, l + w * 0.44f, t + h * 0.58f, h * 0.075f, paint)
    }

    private fun drawTurkey(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.parseColor("#E30A17")
        canvas.drawRect(l, t, l + w, t + h, paint)

        val cy = t + h / 2f
        val cx = l + w * 0.40f
        val r = h * 0.26f

        // White crescent
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, r, paint)
        // Red cutout
        paint.color = Color.parseColor("#E30A17")
        canvas.drawCircle(cx + r * 0.25f, cy, r * 0.80f, paint)

        // Star
        paint.color = Color.WHITE
        drawStar(canvas, l + w * 0.65f, cy, h * 0.12f, paint)
    }

    private fun drawPakistan(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val whiteW = w * 0.25f
        paint.color = Color.WHITE
        canvas.drawRect(l, t, l + whiteW, t + h, paint)
        paint.color = Color.parseColor("#01411C")
        canvas.drawRect(l + whiteW, t, l + w, t + h, paint)

        val greenCenter = l + whiteW + (w - whiteW) / 2f
        val cy = t + h / 2f
        val r = h * 0.26f

        // White crescent
        paint.color = Color.WHITE
        canvas.drawCircle(greenCenter, cy, r, paint)
        // Green cutout offset towards upper-right
        paint.color = Color.parseColor("#01411C")
        canvas.drawCircle(greenCenter + r * 0.25f, cy - r * 0.08f, r * 0.84f, paint)

        // Tilted Star
        paint.color = Color.WHITE
        drawStar(canvas, greenCenter + r * 0.48f, cy - r * 0.25f, h * 0.11f, paint, -18f)
    }

    private fun drawSaudiArabia(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.parseColor("#006C35")
        canvas.drawRect(l, t, l + w, t + h, paint)

        // White sword and script bar
        paint.color = Color.WHITE
        val cy = t + h * 0.64f
        val swordH = h * 0.07f
        canvas.drawRoundRect(l + w * 0.22f, cy - swordH / 2, l + w * 0.78f, cy + swordH / 2, swordH, swordH, paint)

        // White Arabic script emblem block
        val scriptTop = t + h * 0.32f
        canvas.drawRoundRect(l + w * 0.30f, scriptTop, l + w * 0.70f, scriptTop + h * 0.18f, h * 0.05f, h * 0.05f, paint)
    }

    private fun drawIran(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val h3 = h / 3f
        paint.color = Color.parseColor("#239F40")
        canvas.drawRect(l, t, l + w, t + h3, paint)
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(l, t + h3, l + w, t + h3 * 2, paint)
        paint.color = Color.parseColor("#DA0000")
        canvas.drawRect(l, t + h3 * 2, l + w, t + h, paint)

        // Red central emblem
        paint.color = Color.parseColor("#DA0000")
        val cx = l + w / 2f
        val cy = t + h / 2f
        canvas.drawCircle(cx, cy, h * 0.12f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, h * 0.06f, paint)
        paint.color = Color.parseColor("#DA0000")
        canvas.drawCircle(cx, cy, h * 0.03f, paint)
    }

    private fun drawIsrael(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.WHITE
        canvas.drawRect(l, t, l + w, t + h, paint)

        val stripeH = h * 0.13f
        val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0038B8")
            style = Paint.Style.FILL
        }
        canvas.drawRect(l, t + h * 0.12f, l + w, t + h * 0.12f + stripeH, bluePaint)
        canvas.drawRect(l, t + h * 0.75f, l + w, t + h * 0.75f + stripeH, bluePaint)

        // Star of David
        val cx = l + w / 2f
        val cy = t + h / 2f
        val r = h * 0.18f
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0038B8")
            style = Paint.Style.STROKE
            strokeWidth = h * 0.035f
        }
        drawHexagram(canvas, cx, cy, r, starPaint)
    }

    private fun drawHexagram(canvas: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        val p1 = Path()
        for (i in 0 until 3) {
            val a = Math.toRadians((i * 120 - 90).toDouble())
            val x = (cx + r * cos(a)).toFloat()
            val y = (cy + r * sin(a)).toFloat()
            if (i == 0) p1.moveTo(x, y) else p1.lineTo(x, y)
        }
        p1.close()
        canvas.drawPath(p1, p)

        val p2 = Path()
        for (i in 0 until 3) {
            val a = Math.toRadians((i * 120 + 90).toDouble())
            val x = (cx + r * cos(a)).toFloat()
            val y = (cy + r * sin(a)).toFloat()
            if (i == 0) p2.moveTo(x, y) else p2.lineTo(x, y)
        }
        p2.close()
        canvas.drawPath(p2, p)
    }

    private fun drawSouthKorea(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        paint.color = Color.WHITE
        canvas.drawRect(l, t, l + w, t + h, paint)

        val cx = l + w / 2f
        val cy = t + h / 2f
        val r = h * 0.22f

        // Taegeuk: Red top, Blue bottom S-curve
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CD2E3A") }
        val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0047A0") }

        rectF.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rectF, 180f, 180f, true, redPaint)
        canvas.drawArc(rectF, 0f, 180f, true, bluePaint)

        // Inner complementary semicircles
        rectF.set(cx - r, cy - r / 2f, cx, cy + r / 2f)
        canvas.drawArc(rectF, 0f, 360f, true, redPaint)
        rectF.set(cx, cy - r / 2f, cx + r, cy + r / 2f)
        canvas.drawArc(rectF, 0f, 360f, true, bluePaint)

        // Trigram corner lines
        val trigramPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = h * 0.035f
            style = Paint.Style.STROKE
        }
        val trigramOffset = r * 1.55f
        canvas.drawLine(cx - trigramOffset, cy - trigramOffset * 0.5f, cx - trigramOffset + h * 0.14f, cy - trigramOffset * 0.5f, trigramPaint)
        canvas.drawLine(cx + trigramOffset - h * 0.14f, cy - trigramOffset * 0.5f, cx + trigramOffset, cy - trigramOffset * 0.5f, trigramPaint)
        canvas.drawLine(cx - trigramOffset, cy + trigramOffset * 0.5f, cx - trigramOffset + h * 0.14f, cy + trigramOffset * 0.5f, trigramPaint)
        canvas.drawLine(cx + trigramOffset - h * 0.14f, cy + trigramOffset * 0.5f, cx + trigramOffset, cy + trigramOffset * 0.5f, trigramPaint)
    }

    private fun drawMalaysia(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        // 14 red & white horizontal stripes
        val stripeCount = 14
        val sh = h / stripeCount
        for (i in 0 until stripeCount) {
            paint.color = if (i % 2 == 0) Color.parseColor("#CC0000") else Color.WHITE
            canvas.drawRect(l, t + i * sh, l + w, t + (i + 1) * sh, paint)
        }
        // Blue canton
        val cantonW = w * 0.50f
        val cantonH = sh * 8
        paint.color = Color.parseColor("#000066")
        canvas.drawRect(l, t, l + cantonW, t + cantonH, paint)

        // Yellow crescent & star
        val cy = t + cantonH / 2f
        val cx = l + cantonW * 0.36f
        val r = cantonH * 0.34f
        paint.color = Color.parseColor("#FFCC00")
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.parseColor("#000066")
        canvas.drawCircle(cx + r * 0.32f, cy, r * 0.82f, paint)

        paint.color = Color.parseColor("#FFCC00")
        drawStar(canvas, l + cantonW * 0.72f, cy, r * 0.65f, paint, 0f)
    }

    private fun drawPortugal(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        val greenW = w * 0.40f
        paint.color = Color.parseColor("#006600")
        canvas.drawRect(l, t, l + greenW, t + h, paint)
        paint.color = Color.parseColor("#FF0000")
        canvas.drawRect(l + greenW, t, l + w, t + h, paint)

        // Coat of arms at seam
        val cx = l + greenW
        val cy = t + h / 2f
        paint.color = Color.parseColor("#FFCC00")
        canvas.drawCircle(cx, cy, h * 0.20f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, h * 0.12f, paint)
        paint.color = Color.parseColor("#002B7F")
        canvas.drawCircle(cx, cy, h * 0.06f, paint)
    }

    private fun drawUSA(canvas: Canvas, l: Float, t: Float, w: Float, h: Float) {
        // Red and white stripes
        val stripeH = h / 7f
        for (i in 0 until 7) {
            paint.color = if (i % 2 == 0) Color.parseColor("#B22234") else Color.WHITE
            canvas.drawRect(l, t + i * stripeH, l + w, t + (i + 1) * stripeH, paint)
        }
        // Blue canton
        val cw = w * 0.44f
        val ch = stripeH * 4
        paint.color = Color.parseColor("#3C3B6E")
        canvas.drawRect(l, t, l + cw, t + ch, paint)

        // White star dots in canton
        paint.color = Color.WHITE
        val dotR = h * 0.032f
        val cols = 3
        val rows = 3
        for (c in 1..cols) {
            for (r in 1..rows) {
                val dx = l + cw * (c / (cols + 1f))
                val dy = t + ch * (r / (rows + 1f))
                canvas.drawCircle(dx, dy, dotR, paint)
            }
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, p: Paint, rotationDeg: Float = 0f) {
        val innerR = r * 0.382f
        shapePath.reset()
        val totalPoints = 10
        val rotRad = Math.toRadians(rotationDeg.toDouble() - 90.0)
        for (i in 0 until totalPoints) {
            val curR = if (i % 2 == 0) r else innerR
            val a = rotRad + i * Math.PI / 5.0
            val x = (cx + curR * cos(a)).toFloat()
            val y = (cy + curR * sin(a)).toFloat()
            if (i == 0) shapePath.moveTo(x, y) else shapePath.lineTo(x, y)
        }
        shapePath.close()
        canvas.drawPath(shapePath, p)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = 72
    override fun getIntrinsicHeight(): Int = 52
}
