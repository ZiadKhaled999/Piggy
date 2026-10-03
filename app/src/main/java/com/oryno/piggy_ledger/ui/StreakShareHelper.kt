package com.oryno.piggy_ledger.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.StreakManager
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Calendar
import java.util.Locale

object StreakShareHelper {

    fun createStreakImageBitmap(context: Context, streakCount: Int): Bitmap {
        return when {
            streakCount in 7..99 -> createBalloonStreakBitmap(context, streakCount)
            streakCount >= 100 -> createDay100ActivePiggyStreakBitmap(context, streakCount)
            else -> createStarterStreakBitmap(context, maxOf(1, streakCount))
        }
    }

    /**
     * Tier 1 (Days 1 to 6): The Starter Card
     * Clean white card, soft pink grid, pink aura, 3D Piggy face, bold 3D number.
     */
    private fun createStarterStreakBitmap(context: Context, streakCount: Int): Bitmap {
        val width = 1080
        val height = 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer Canvas Background (Soft Neutral)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        // Main White Card with Rounded Corners
        val cardMargin = 50f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardRadius = 60f

        // Card Shadow/Glow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10F43F5E")
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(cardRect.left - 10f, cardRect.top - 10f, cardRect.right + 10f, cardRect.bottom + 10f)
        canvas.drawRoundRect(shadowRect, cardRadius + 10f, cardRadius + 10f, shadowPaint)

        // Card Fill
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // Card Border (Soft Pink)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBCFE8")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, strokePaint)

        // Subtle Pink Grid Pattern on Card
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCE7F3")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val gridSize = 80f
        var gx = cardRect.left + gridSize
        while (gx < cardRect.right) {
            canvas.drawLine(gx, cardRect.top, gx, cardRect.bottom, gridPaint)
            gx += gridSize
        }
        var gy = cardRect.top + gridSize
        while (gy < cardRect.bottom) {
            canvas.drawLine(cardRect.left, gy, cardRect.right, gy, gridPaint)
            gy += gridSize
        }

        // Header Text "Piggy Ledger"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Piggy Ledger", width / 2f, cardRect.top + 110f, headerPaint)

        // Center Radial Pink Glow behind Mascot
        val glowCenterX = width / 2f
        val glowCenterY = cardRect.top + 410f
        val glowRadius = 400f
        val glowShader = RadialGradient(
            glowCenterX,
            glowCenterY,
            glowRadius,
            intArrayOf(
                Color.parseColor("#80F43F5E"),
                Color.parseColor("#30FB7185"),
                Color.parseColor("#00FFFFFF")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = glowShader
        }
        canvas.drawCircle(glowCenterX, glowCenterY, glowRadius, glowPaint)

        // Draw Sparkle Accents in Pink
        drawSparkle(canvas, glowCenterX - 340f, glowCenterY - 160f, 36f, Color.parseColor("#F43F5E"))
        drawSparkle(canvas, glowCenterX + 330f, glowCenterY - 130f, 32f, Color.parseColor("#FB7185"))
        drawSparkle(canvas, glowCenterX - 310f, glowCenterY + 180f, 40f, Color.parseColor("#F43F5E"))
        drawSparkle(canvas, glowCenterX + 340f, glowCenterY + 160f, 34f, Color.parseColor("#FB7185"))

        // Draw Piggy Mascot Logo Image
        val mascotDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.img_app_logo)
            ?: ContextCompat.getDrawable(context, R.drawable.streak)
        if (mascotDrawable != null) {
            val mascotSize = 540
            val left = ((width - mascotSize) / 2)
            val top = (glowCenterY - mascotSize / 2f).toInt() - 10
            mascotDrawable.setBounds(left, top, left + mascotSize, top + mascotSize)
            mascotDrawable.draw(canvas)
        }

        // Draw Streak Number in Giant Pink Bold 3D Font
        val numberText = "$streakCount"
        val numberY = cardRect.top + 770f

        // 3D Shadow for Number
        val numberShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9F1239")
            textSize = 190f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberText, width / 2f + 8f, numberY + 8f, numberShadowPaint)

        // Main Number Fill (Bright Pink Gradient)
        val numberShader = LinearGradient(
            width / 2f, numberY - 150f, width / 2f, numberY,
            Color.parseColor("#FB7185"),
            Color.parseColor("#E11D48"),
            Shader.TileMode.CLAMP
        )
        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = numberShader
            textSize = 190f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberText, width / 2f, numberY, numberPaint)

        // Subtitle Text: "day saving streak"
        val subtitleText = context.getString(if (streakCount == 1) R.string.streak_day_saving_streak else R.string.streak_days_saving_streak)
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BE185D")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitleText, width / 2f, numberY + 70f, subtitlePaint)

        // Statement Text Box
        val statementText = getShareStatement(context, streakCount)
        drawStatementBox(canvas, cardRect, statementText, numberY + 180f, width)

        return bitmap
    }

    /**
     * Tier 2 (Days 7 to 99): The Balloon Liftoff Card
     * Piggy in full-body pose holding balloon strings, with 3D metallic Rose-Gold balloons
     * displaying the exact streak day number (7 to 99), festive confetti, and celebratory atmosphere.
     */
    private fun createBalloonStreakBitmap(context: Context, streakCount: Int): Bitmap {
        val width = 1080
        val height = 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer Canvas Background: Soft Warm Pastel Sky
        canvas.drawColor(Color.parseColor("#FFF5F7"))

        val cardMargin = 50f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardRadius = 60f

        // Card Glow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18FB7185")
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(cardRect.left - 12f, cardRect.top - 12f, cardRect.right + 12f, cardRect.bottom + 12f)
        canvas.drawRoundRect(shadowRect, cardRadius + 12f, cardRadius + 12f, shadowPaint)

        // Main White Card Fill
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // Card Border in Radiant Rose-Gold
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDA4AF")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

        // Header Text "Piggy Ledger" (Clean, spacious top header without any subtitle below it)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Piggy Ledger", width / 2f, cardRect.top + 85f, headerPaint)

        // Floating Festive Confetti Dots & Shapes
        drawConfetti(canvas, cardRect)

        // Mascot Dimensions (proportional to 433 x 576 aspect ratio)
        val mascotWidth = 400f
        val mascotHeight = mascotWidth * (576f / 433f) // ~532f
        val mascotLeft = (width - mascotWidth) / 2f // 340f
        val mascotTop = cardRect.top + 470f // 520f

        // Exact anchor coordinate of Piggy's raised hoof/hand
        val handX = mascotLeft + mascotWidth * 0.476f // ~530f (centered)
        val handY = mascotTop + mascotHeight * 0.096f // ~571f

        // Draw 3D Inflatable Rose-Gold Balloons for streakCount
        val digits = streakCount.toString()
        val balloonCenterY = cardRect.top + 260f

        if (digits.length == 1) {
            // Single Digit Balloon (e.g. 7, 8, 9)
            val balloonCenterX = width / 2f
            val balloonWidth = 230f
            val balloonHeight = 300f
            val knotX = balloonCenterX
            val knotY = balloonCenterY + balloonHeight / 2f - 8f // Bottom knot

            // Draw Ribbon String connecting knot directly to Piggy's raised hoof
            drawBalloonString(canvas, knotX, knotY, handX, handY, curveFactor = -15f)

            // Draw 3D Rose-Gold Balloon Digit
            draw3dRoseGoldBalloonDigit(canvas, digits[0], balloonCenterX, balloonCenterY, balloonWidth, balloonHeight, 0f)
        } else {
            // Two Digit Balloons (10 to 99)
            val spacing = 205f
            val leftBalloonX = width / 2f - spacing / 2f
            val rightBalloonX = width / 2f + spacing / 2f
            val balloonWidth = 185f
            val balloonHeight = 265f

            val leftKnotX = leftBalloonX
            val leftKnotY = balloonCenterY + balloonHeight / 2f - 8f
            val rightKnotX = rightBalloonX
            val rightKnotY = balloonCenterY + balloonHeight / 2f - 8f

            // Draw two elegant curved ribbon strings converging directly into Piggy's raised hoof
            drawBalloonString(canvas, leftKnotX, leftKnotY, handX, handY, curveFactor = -35f)
            drawBalloonString(canvas, rightKnotX, rightKnotY, handX, handY, curveFactor = 35f)

            // Draw 3D Inflatable Rose-Gold Balloon Digits with slight festive tilt
            draw3dRoseGoldBalloonDigit(canvas, digits[0], leftBalloonX, balloonCenterY, balloonWidth, balloonHeight, -7f)
            draw3dRoseGoldBalloonDigit(canvas, digits[1], rightBalloonX, balloonCenterY, balloonWidth, balloonHeight, 7f)
        }

        // Draw Mascot
        val mascotDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.img_piggy_balloons_pose)
            ?: ContextCompat.getDrawable(context, R.drawable.img_app_logo)
        if (mascotDrawable != null) {
            mascotDrawable.setBounds(
                mascotLeft.toInt(),
                mascotTop.toInt(),
                (mascotLeft + mascotWidth).toInt(),
                (mascotTop + mascotHeight).toInt()
            )
            mascotDrawable.draw(canvas)
        }

        // Ribbon Tie Grip Accent right on the hoof (shows the ribbons physically grasped in hand)
        drawHandRibbonGrip(canvas, handX, handY)

        // Subtitle Text: "$streakCount days saving streak"
        val subtitleY = cardRect.top + 1035f
        val subtitleText = context.getString(if (streakCount == 1) R.string.streak_day_saving_streak else R.string.streak_days_saving_streak)
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BE185D")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$streakCount $subtitleText", width / 2f, subtitleY, subtitlePaint)

        // Statement Text Box
        val statementText = getShareStatement(context, streakCount)
        drawStatementBox(canvas, cardRect, statementText, subtitleY + 65f, width)

        return bitmap
    }

    /**
     * Tier 3 (Day 100+): The Elite Century Milestone Card
     * Dynamic parallel presentation in Piggy Ledger signature color scheme:
     * - Left side: Huge-scale Active Piggy mascot bursting in from the left, presenting the achievement
     * - Right side: Parallel Special Huge 3D Majestic Streak Number (Serif bold metallic ruby & rose-gold)
     * - Lower area: Highlighted celebratory motivational card box
     * - Bottom-right: "Piggy Ledger" brand title
     * - Clean, spacious, perfectly organized with zero cluttered subtitles
     */
    private fun createDay100ActivePiggyStreakBitmap(context: Context, streakCount: Int): Bitmap {
        val width = 1080
        val height = 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Outer Canvas Background: Soft Warm Pastel Sky (Piggy Theme)
        canvas.drawColor(Color.parseColor("#FFF5F7"))

        val cardMargin = 50f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardRadius = 60f

        // 2. Card Shadow / Glow (Soft Rose-Gold Aura)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18FB7185")
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(cardRect.left - 12f, cardRect.top - 12f, cardRect.right + 12f, cardRect.bottom + 12f)
        canvas.drawRoundRect(shadowRect, cardRadius + 12f, cardRadius + 12f, shadowPaint)

        // 3. Main White Card Fill
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // 4. Delicate Rose-Gold Card Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBCFE8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

        // 5. Check Locale for RTL (Arabic vs English)
        val isArabic = Locale.getDefault().language.startsWith("ar")

        // 6. Coordinates for Parallel Presentation (Mascot on left, Number on right)
        val mascotCenterX = if (isArabic) cardRect.right - 230f else cardRect.left + 230f
        val mascotCenterY = cardRect.top + 390f

        val numberCenterX = if (isArabic) cardRect.left + 260f else cardRect.right - 260f
        val numberCenterY = cardRect.top + 390f

        // 7. Radiant Soft Glow behind Mascot (Warm Rose & Gold Aura)
        val glowRadius = 270f
        val glowShader = RadialGradient(
            mascotCenterX,
            mascotCenterY,
            glowRadius,
            intArrayOf(
                Color.parseColor("#45FB7185"),
                Color.parseColor("#18F59E0B"),
                Color.parseColor("#00FFFFFF")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = glowShader
        }
        canvas.drawCircle(mascotCenterX, mascotCenterY, glowRadius, glowPaint)

        // Festive Sparkles around Mascot
        val sparkleSign = if (isArabic) -1f else 1f
        drawSparkle(canvas, mascotCenterX - 200f * sparkleSign, mascotCenterY - 210f, 32f, Color.parseColor("#F43F5E"))
        drawSparkle(canvas, mascotCenterX + 220f * sparkleSign, mascotCenterY - 180f, 36f, Color.parseColor("#F59E0B"))
        drawSparkle(canvas, mascotCenterX - 210f * sparkleSign, mascotCenterY + 220f, 28f, Color.parseColor("#F59E0B"))
        drawSparkle(canvas, mascotCenterX + 220f * sparkleSign, mascotCenterY + 200f, 30f, Color.parseColor("#FB7185"))

        // 8. Draw Active Piggy Mascot on Left Side (Huge Scale, Presenting Outward)
        val activePiggyResId = context.resources.getIdentifier("active_piggy", "drawable", context.packageName)
        val mascotDrawable: Drawable? = if (activePiggyResId != 0) {
            ContextCompat.getDrawable(context, activePiggyResId)
        } else {
            ContextCompat.getDrawable(context, R.drawable.img_piggy_active_trans)
                ?: ContextCompat.getDrawable(context, R.drawable.streak)
                ?: ContextCompat.getDrawable(context, R.drawable.img_piggy_shades_flame)
                ?: ContextCompat.getDrawable(context, R.drawable.img_app_logo)
        }

        if (mascotDrawable != null) {
            val maxMascotSize = 510f
            val intrinsicW = mascotDrawable.intrinsicWidth.toFloat()
            val intrinsicH = mascotDrawable.intrinsicHeight.toFloat()
            val scale = if (intrinsicW > 0 && intrinsicH > 0) {
                minOf(maxMascotSize / intrinsicW, maxMascotSize / intrinsicH)
            } else 1f
            val actualW = if (intrinsicW > 0) intrinsicW * scale else maxMascotSize
            val actualH = if (intrinsicH > 0) intrinsicH * scale else maxMascotSize

            val left = (mascotCenterX - actualW / 2f).toInt()
            val top = (mascotCenterY - actualH / 2f).toInt()
            mascotDrawable.setBounds(left, top, (left + actualW).toInt(), (top + actualH).toInt())
            mascotDrawable.draw(canvas)
        }

        // 9. Parallel Right Side: Pure SPECIAL HUGE 3D MAJESTIC NUMERIC
        val numberText = "$streakCount"
        val numberBaseY = numberCenterY + 75f
        drawSpecialHuge3dStreakNumber(canvas, numberText, numberCenterX, numberBaseY)

        // 10. Highlighted Celebratory Milestone Statement Box (Scaled up, bigger bold font)
        val statementText = getShareStatement(context, streakCount)
        val statementStartY = cardRect.top + 730f
        drawHighlightedMotivationalBox(canvas, cardRect, statementText, statementStartY, width)

        // 11. App Name placed in the Bottom-Right (or Bottom-Left in RTL)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = if (isArabic) Paint.Align.LEFT else Paint.Align.RIGHT
            letterSpacing = 0.04f
        }
        val brandX = if (isArabic) cardRect.left + 45f else cardRect.right - 45f
        val brandY = cardRect.bottom - 38f
        canvas.drawText("Piggy Ledger", brandX, brandY, brandPaint)

        return bitmap
    }

    /**
     * Renders the Day 100+ Special Huge 3D Majestic Celebratory Numeric in the Piggy Ledger color scheme.
     * Uses a majestic serif bold typeface, multi-layer 3D extrusion, metallic rose-gold/ruby foil gradient,
     * specular gloss, and festive glints.
     */
    private fun drawSpecialHuge3dStreakNumber(
        canvas: Canvas,
        text: String,
        cx: Float,
        baseY: Float
    ) {
        val textSize = when {
            text.length >= 4 -> 180f
            text.length == 3 -> 220f
            else -> 245f
        }

        val majesticTypeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

        // 1. Deep Ambient Soft Shadow
        val ambientShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25881337")
            this.textSize = textSize
            typeface = majesticTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, cx + 8f, baseY + 20f, ambientShadowPaint)

        // 2. Multi-Layer Stepped 3D Extrusion (Creates physical 3D block thickness)
        val extrudePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#881337")
            this.textSize = textSize
            typeface = majesticTypeface
            textAlign = Paint.Align.CENTER
        }
        for (i in 10 downTo 1) {
            val stepColor = if (i > 5) "#881337" else "#9F1239"
            extrudePaint.color = Color.parseColor(stepColor)
            canvas.drawText(text, cx, baseY + (i * 1.6f), extrudePaint)
        }

        // 3. Crisp Front Rim / Bevel
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BE185D")
            this.textSize = textSize
            typeface = majesticTypeface
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 6.5f
        }
        canvas.drawText(text, cx, baseY, rimPaint)

        // 4. Main Front Face: Metallic Piggy Rose-Gold & Ruby Gradient
        val faceShader = LinearGradient(
            cx, baseY - textSize, cx, baseY,
            intArrayOf(
                Color.parseColor("#FFF1F2"), // Specular rim
                Color.parseColor("#FECDD3"), // Rose gold highlight
                Color.parseColor("#FB7185"), // Signature Piggy pink
                Color.parseColor("#F43F5E"), // Rose coral
                Color.parseColor("#E11D48"), // Deep ruby
                Color.parseColor("#BE185D")  // Bottom shadow
            ),
            floatArrayOf(0f, 0.15f, 0.40f, 0.65f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = faceShader
            this.textSize = textSize
            typeface = majesticTypeface
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
        canvas.drawText(text, cx, baseY, facePaint)

        // 5. Specular Top Highlight (Gives glossy reflective sheen on the top curve)
        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0FFFFFF")
            this.textSize = textSize
            typeface = majesticTypeface
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
        }
        canvas.save()
        canvas.translate(0f, -2f)
        canvas.drawText(text, cx, baseY, glossPaint)
        canvas.restore()

        // 6. Celebratory Sparkles on the numeral
        drawSparkle(canvas, cx + (text.length * textSize * 0.22f), baseY - textSize * 0.80f, 28f, Color.parseColor("#F59E0B"))
        drawSparkle(canvas, cx - (text.length * textSize * 0.24f), baseY - textSize * 0.42f, 24f, Color.parseColor("#FB7185"))
    }

    /**
     * Renders the prominent, scaled-up, highlighted celebratory motivational box for Day 100+.
     */
    private fun drawHighlightedMotivationalBox(
        canvas: Canvas,
        cardRect: RectF,
        statementText: String,
        startY: Float,
        canvasWidth: Int
    ) {
        val statementPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val maxWidth = cardRect.width() - 140f
        val lines = wrapText(statementText, statementPaint, maxWidth)

        val pillPaddingV = 36f
        val lineHeight = 58f
        val pillHeight = (lines.size * lineHeight) + (pillPaddingV * 2) - 10f
        val pillRect = RectF(
            cardRect.left + 32f,
            startY,
            cardRect.right - 32f,
            minOf(startY + pillHeight, cardRect.bottom - 75f)
        )

        // Highlighted Soft Rose-Gold Gradient Background
        val pillShader = LinearGradient(
            pillRect.left, pillRect.top, pillRect.right, pillRect.bottom,
            intArrayOf(
                Color.parseColor("#FFF1F2"), // Soft rose tint
                Color.parseColor("#FFE4E6")  // Warm peach-rose
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = pillShader
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDA4AF") // Crisp rose-gold border
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(pillRect, 36f, 36f, bgPaint)
        canvas.drawRoundRect(pillRect, 36f, 36f, borderPaint)

        // Celebratory sparkles on motivational box corners
        drawSparkle(canvas, pillRect.left + 38f, pillRect.top + 28f, 20f, Color.parseColor("#F59E0B"))
        drawSparkle(canvas, pillRect.right - 38f, pillRect.bottom - 28f, 20f, Color.parseColor("#F43F5E"))

        var lineY = startY + pillPaddingV + 38f
        for (line in lines) {
            canvas.drawText(line, canvasWidth / 2f, lineY, statementPaint)
            lineY += lineHeight
        }
    }

    /**
     * Renders a true 3D Inflatable Rose-Gold Foil Balloon Digit with bulbous tubular anatomy,
     * multi-layer cylindrical shading, specular foil reflections, and authentic 3D valve tie knots.
     */
    private fun draw3dRoseGoldBalloonDigit(
        canvas: Canvas,
        digitChar: Char,
        cx: Float,
        cy: Float,
        balloonWidth: Float,
        balloonHeight: Float,
        rotationDeg: Float
    ) {
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)

        val left = cx - balloonWidth / 2f
        val top = cy - balloonHeight / 2f
        val right = cx + balloonWidth / 2f
        val bottom = cy + balloonHeight / 2f

        val digitPath = getBalloonDigitCenterlinePath(digitChar, left, top, balloonWidth, balloonHeight)

        // 1. Layer 1: Ambient Occlusion Outer Drop Shadow
        val shadowPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18500724")
            style = Paint.Style.STROKE
            strokeWidth = 84f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(10f, 20f)
        canvas.drawPath(digitPath, shadowPaint1)
        canvas.restore()

        // 2. Layer 2: Core Drop Shadow (Tighter Depth)
        val shadowPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38701A75")
            style = Paint.Style.STROKE
            strokeWidth = 72f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(6f, 12f)
        canvas.drawPath(digitPath, shadowPaint2)
        canvas.restore()

        // 3. Layer 3: Foil Seam / Under-Crease Rim (Deep Metallic Crimson/Burgundy)
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#881337")
            style = Paint.Style.STROKE
            strokeWidth = 70f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(digitPath, rimPaint)

        // 4. Layer 4: Main Inflatable Metallic Rose-Gold Foil Body
        val foilShader = LinearGradient(
            left, top, right, bottom,
            intArrayOf(
                Color.parseColor("#FFF1F2"), // Specular rim reflection
                Color.parseColor("#FECDD3"), // Rose gold reflection
                Color.parseColor("#FB7185"), // Saturated rose coral
                Color.parseColor("#F43F5E"), // Deep rich rose
                Color.parseColor("#BE185D"), // Shadowed foil depth
                Color.parseColor("#9F1239")  // Deep underside crease
            ),
            floatArrayOf(0f, 0.15f, 0.40f, 0.65f, 0.88f, 1f),
            Shader.TileMode.CLAMP
        )
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = foilShader
            style = Paint.Style.STROKE
            strokeWidth = 64f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(digitPath, bodyPaint)

        // 5. Layer 5: Cylindrical 3D Inflation Core Highlight (Adds the bulbous cushion volume)
        val coreShader = LinearGradient(
            left - 10f, top - 15f, right - 10f, bottom - 15f,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#FFE4E6"),
                Color.parseColor("#FDA4AF"),
                Color.parseColor("#E11D48")
            ),
            floatArrayOf(0f, 0.25f, 0.60f, 1f),
            Shader.TileMode.CLAMP
        )
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = coreShader
            style = Paint.Style.STROKE
            strokeWidth = 38f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(digitPath, corePaint)

        // 6. Layer 6: Mylar Specular Gloss Highlight (High-gloss reflective sheen on top/left curve)
        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D9FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(-4f, -7f)
        canvas.drawPath(getBalloonGlossHighlightPath(digitChar, left, top, balloonWidth, balloonHeight), glossPaint)
        canvas.restore()

        // 7. Layer 7: Specular Glint Starbursts (Authentic photographic studio gleam)
        drawBalloonGlints(canvas, digitChar, left, top, balloonWidth, balloonHeight)

        // 8. Layer 8: 3D Foil Balloon Tie / Valve at bottom of the digit
        drawBalloonValveKnot(canvas, cx, bottom - 10f)

        canvas.restore()
    }

    /**
     * Constructs a smooth, chubby centerline path for digits 0-9 optimized for tubular 3D inflation.
     */
    private fun getBalloonDigitCenterlinePath(
        digit: Char,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ): Path {
        val path = Path()
        val right = left + w
        val bottom = top + h
        val cx = left + w / 2f
        val cy = top + h / 2f

        when (digit) {
            '0' -> {
                // Round puffy torus/donut oval
                val ovalRect = RectF(left + w * 0.20f, top + h * 0.16f, left + w * 0.80f, top + h * 0.84f)
                path.addOval(ovalRect, Path.Direction.CW)
            }
            '1' -> {
                // Chubby rounded 1 with playful top beak and straight stem
                path.moveTo(left + w * 0.24f, top + h * 0.28f)
                path.lineTo(cx, top + h * 0.15f)
                path.lineTo(cx, top + h * 0.85f)
            }
            '2' -> {
                // Puffy curved top, gentle diagonal sweep, flat rounded base
                path.moveTo(left + w * 0.20f, top + h * 0.28f)
                path.cubicTo(left + w * 0.20f, top + h * 0.12f, left + w * 0.80f, top + h * 0.12f, left + w * 0.80f, top + h * 0.32f)
                path.cubicTo(left + w * 0.80f, top + h * 0.52f, left + w * 0.22f, top + h * 0.68f, left + w * 0.20f, top + h * 0.85f)
                path.lineTo(left + w * 0.82f, top + h * 0.85f)
            }
            '3' -> {
                // Double curved lobes with bouncy waist
                path.moveTo(left + w * 0.22f, top + h * 0.20f)
                path.cubicTo(left + w * 0.24f, top + h * 0.12f, left + w * 0.80f, top + h * 0.12f, left + w * 0.80f, top + h * 0.32f)
                path.cubicTo(left + w * 0.80f, top + h * 0.48f, cx + w * 0.05f, cy, cx + w * 0.05f, cy)
                path.cubicTo(left + w * 0.82f, cy, left + w * 0.82f, top + h * 0.86f, cx, top + h * 0.86f)
                path.cubicTo(left + w * 0.28f, top + h * 0.86f, left + w * 0.20f, top + h * 0.74f, left + w * 0.20f, top + h * 0.70f)
            }
            '4' -> {
                // Diagonal arm, horizontal shelf, bold vertical stem
                path.moveTo(left + w * 0.68f, top + h * 0.14f)
                path.lineTo(left + w * 0.18f, top + h * 0.62f)
                path.lineTo(left + w * 0.84f, top + h * 0.62f)
                path.moveTo(left + w * 0.68f, top + h * 0.14f)
                path.lineTo(left + w * 0.68f, top + h * 0.86f)
            }
            '5' -> {
                // Flat top bar, short neck, giant bouncy belly
                path.moveTo(left + w * 0.78f, top + h * 0.15f)
                path.lineTo(left + w * 0.24f, top + h * 0.15f)
                path.lineTo(left + w * 0.24f, top + h * 0.44f)
                path.cubicTo(left + w * 0.32f, top + h * 0.38f, left + w * 0.82f, top + h * 0.38f, left + w * 0.82f, top + h * 0.66f)
                path.cubicTo(left + w * 0.82f, top + h * 0.86f, left + w * 0.30f, top + h * 0.86f, left + w * 0.20f, top + h * 0.74f)
            }
            '6' -> {
                // Smooth outer swoop looping into chubby round bottom loop
                path.moveTo(left + w * 0.74f, top + h * 0.20f)
                path.cubicTo(left + w * 0.20f, top + h * 0.24f, left + w * 0.16f, top + h * 0.86f, cx, top + h * 0.86f)
                path.cubicTo(left + w * 0.82f, top + h * 0.86f, left + w * 0.82f, cy - h * 0.02f, cx, cy - h * 0.02f)
                path.cubicTo(left + w * 0.18f, cy - h * 0.02f, left + w * 0.18f, top + h * 0.62f, left + w * 0.18f, top + h * 0.62f)
            }
            '7' -> {
                // High horizontal bar, bold diagonal sweeping down to anchor
                path.moveTo(left + w * 0.18f, top + h * 0.15f)
                path.lineTo(left + w * 0.82f, top + h * 0.15f)
                path.lineTo(left + w * 0.40f, top + h * 0.85f)
            }
            '8' -> {
                // Figure-8 with two rounded interconnected lobes
                val topOval = RectF(cx - w * 0.28f, top + h * 0.14f, cx + w * 0.28f, cy + h * 0.03f)
                val botOval = RectF(cx - w * 0.33f, cy - h * 0.03f, cx + w * 0.33f, top + h * 0.86f)
                path.addOval(topOval, Path.Direction.CW)
                path.addOval(botOval, Path.Direction.CW)
            }
            '9' -> {
                // Chubby top loop sweeping gracefully down the right side
                path.moveTo(left + w * 0.26f, top + h * 0.80f)
                path.cubicTo(left + w * 0.80f, top + h * 0.76f, left + w * 0.84f, top + h * 0.14f, cx, top + h * 0.14f)
                path.cubicTo(left + w * 0.18f, top + h * 0.14f, left + w * 0.18f, cy + h * 0.02f, cx, cy + h * 0.02f)
                path.cubicTo(left + w * 0.82f, cy + h * 0.02f, left + w * 0.82f, top + h * 0.38f, left + w * 0.82f, top + h * 0.38f)
            }
            else -> {
                // Generic rounded pill for unexpected characters
                path.addRoundRect(RectF(left + w * 0.25f, top + h * 0.15f, right - w * 0.25f, bottom - h * 0.15f), 30f, 30f, Path.Direction.CW)
            }
        }
        return path
    }

    /**
     * Builds the specular highlight stroke path running along the upper-left crest of the balloon.
     */
    private fun getBalloonGlossHighlightPath(
        digit: Char,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ): Path {
        val path = Path()
        val right = left + w
        val bottom = top + h
        val cx = left + w / 2f
        val cy = top + h / 2f

        when (digit) {
            '0' -> {
                path.addArc(RectF(left + w * 0.20f, top + h * 0.16f, left + w * 0.80f, top + h * 0.84f), 170f, 100f)
            }
            '1' -> {
                path.moveTo(left + w * 0.28f, top + h * 0.26f)
                path.lineTo(cx - 3f, top + h * 0.18f)
                path.lineTo(cx - 3f, top + h * 0.55f)
            }
            '2' -> {
                path.moveTo(left + w * 0.26f, top + h * 0.26f)
                path.cubicTo(left + w * 0.26f, top + h * 0.16f, left + w * 0.74f, top + h * 0.16f, left + w * 0.74f, top + h * 0.28f)
            }
            '3' -> {
                path.moveTo(left + w * 0.28f, top + h * 0.20f)
                path.cubicTo(left + w * 0.30f, top + h * 0.14f, left + w * 0.74f, top + h * 0.14f, left + w * 0.74f, top + h * 0.26f)
            }
            '4' -> {
                path.moveTo(left + w * 0.65f, top + h * 0.18f)
                path.lineTo(left + w * 0.25f, top + h * 0.58f)
            }
            '5' -> {
                path.moveTo(left + w * 0.72f, top + h * 0.17f)
                path.lineTo(left + w * 0.26f, top + h * 0.17f)
                path.lineTo(left + w * 0.26f, top + h * 0.42f)
            }
            '6' -> {
                path.moveTo(left + w * 0.68f, top + h * 0.22f)
                path.cubicTo(left + w * 0.24f, top + h * 0.28f, left + w * 0.20f, cy, left + w * 0.20f, cy + h * 0.15f)
            }
            '7' -> {
                path.moveTo(left + w * 0.22f, top + h * 0.16f)
                path.lineTo(left + w * 0.78f, top + h * 0.16f)
            }
            '8' -> {
                path.addArc(RectF(cx - w * 0.26f, top + h * 0.16f, cx + w * 0.26f, cy + h * 0.01f), 170f, 100f)
            }
            '9' -> {
                path.addArc(RectF(cx - w * 0.28f, top + h * 0.16f, cx + w * 0.28f, cy + h * 0.04f), 160f, 110f)
            }
            else -> {
                path.moveTo(left + w * 0.3f, top + h * 0.2f)
                path.lineTo(right - w * 0.3f, top + h * 0.2f)
            }
        }
        return path
    }

    /**
     * Draws brilliant specular glints (photographic reflections) on the peak highlights.
     */
    private fun drawBalloonGlints(
        canvas: Canvas,
        digit: Char,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ) {
        val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val glintX = left + w * 0.35f
        val glintY = top + h * 0.20f

        // Center dot
        canvas.drawCircle(glintX, glintY, 5f, glintPaint)

        // Micro sparkle cross
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E6FFFFFF")
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(glintX - 10f, glintY, glintX + 10f, glintY, sparklePaint)
        canvas.drawLine(glintX, glintY - 10f, glintX, glintY + 10f, sparklePaint)
    }

    /**
     * Draws the authentic 3D tied foil valve / nozzle at the bottom of the balloon.
     */
    private fun drawBalloonValveKnot(canvas: Canvas, cx: Float, knotY: Float) {
        // 1. Flared triangular foil tab
        val tabPath = Path().apply {
            moveTo(cx, knotY - 4f)
            lineTo(cx - 18f, knotY + 18f)
            lineTo(cx + 18f, knotY + 18f)
            close()
        }
        val tabShader = LinearGradient(
            cx - 18f, knotY, cx + 18f, knotY + 18f,
            Color.parseColor("#FDA4AF"),
            Color.parseColor("#BE185D"),
            Shader.TileMode.CLAMP
        )
        val tabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = tabShader
            style = Paint.Style.FILL
        }
        canvas.drawPath(tabPath, tabPaint)

        // 2. Foil knot rim shadow
        val tabBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#881337")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawPath(tabPath, tabBorder)

        // 3. Central tied knot sphere
        val knotSphereShader = RadialGradient(
            cx - 2f, knotY + 6f, 10f,
            intArrayOf(Color.parseColor("#FFF1F2"), Color.parseColor("#FB7185"), Color.parseColor("#881337")),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val knotSpherePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = knotSphereShader
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, knotY + 8f, 7f, knotSpherePaint)
    }

    /**
     * Draws a delicate curved metallic ribbon string from the balloon knot to Piggy's raised hoof.
     */
    private fun drawBalloonString(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        curveFactor: Float = 0f
    ) {
        val stringPath = Path().apply {
            moveTo(startX, startY)
            val midX = (startX + endX) / 2f + curveFactor
            val midY = (startY + endY) / 2f
            quadTo(midX, midY, endX, endY)
        }

        // String shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#209F1239")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawPath(stringPath, shadowPaint)

        // Main Rose-Gold Ribbon String
        val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FB7185")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawPath(stringPath, stringPaint)
    }

    /**
     * Draws a delicate metallic rose-gold knot where the ribbons are gathered into Piggy's raised hoof.
     */
    private fun drawHandRibbonGrip(canvas: Canvas, handX: Float, handY: Float) {
        val knotShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40701A75")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(handX + 1.5f, handY + 2.5f, 9f, knotShadowPaint)

        val knotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                handX - 2f, handY - 2f, 10f,
                intArrayOf(Color.parseColor("#FFF1F2"), Color.parseColor("#FB7185"), Color.parseColor("#BE185D")),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(handX, handY, 8f, knotPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#881337")
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        canvas.drawCircle(handX, handY, 8f, rimPaint)
    }

    /**
     * Draws celebratory confetti scattered across the card for Tier 2.
     */
    private fun drawConfetti(canvas: Canvas, cardRect: RectF) {
        val confettiColors = intArrayOf(
            Color.parseColor("#FDA4AF"),
            Color.parseColor("#F43F5E"),
            Color.parseColor("#FDE047"),
            Color.parseColor("#6EE7B7"),
            Color.parseColor("#93C5FD"),
            Color.parseColor("#C084FC")
        )

        val positions = arrayOf(
            floatArrayOf(cardRect.left + 80f, cardRect.top + 220f, 14f, 0f),
            floatArrayOf(cardRect.left + 140f, cardRect.top + 290f, 10f, 1f),
            floatArrayOf(cardRect.right - 90f, cardRect.top + 210f, 16f, 2f),
            floatArrayOf(cardRect.right - 150f, cardRect.top + 280f, 12f, 3f),
            floatArrayOf(cardRect.left + 90f, cardRect.top + 450f, 12f, 4f),
            floatArrayOf(cardRect.right - 100f, cardRect.top + 460f, 15f, 5f),
            floatArrayOf(cardRect.left + 160f, cardRect.top + 600f, 11f, 1f),
            floatArrayOf(cardRect.right - 120f, cardRect.top + 620f, 13f, 0f)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (pos in positions) {
            paint.color = confettiColors[(pos[3].toInt()) % confettiColors.size]
            canvas.drawCircle(pos[0], pos[1], pos[2], paint)
        }

        // Draw celebratory stars
        drawSparkle(canvas, cardRect.left + 120f, cardRect.top + 190f, 24f, Color.parseColor("#F43F5E"))
        drawSparkle(canvas, cardRect.right - 110f, cardRect.top + 180f, 26f, Color.parseColor("#FB7185"))
        drawSparkle(canvas, cardRect.left + 90f, cardRect.top + 380f, 20f, Color.parseColor("#FDE047"))
        drawSparkle(canvas, cardRect.right - 80f, cardRect.top + 390f, 22f, Color.parseColor("#FDE047"))
    }

    /**
     * Draws procedural starry cosmic sky for Tier 3.
     */
    private fun drawStarfield(canvas: Canvas, cardRect: RectF) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val starPositions = arrayOf(
            floatArrayOf(120f, 180f, 4f, 220f),
            floatArrayOf(240f, 230f, 3f, 180f),
            floatArrayOf(160f, 340f, 5f, 255f),
            floatArrayOf(90f, 480f, 3f, 160f),
            floatArrayOf(180f, 620f, 4f, 200f),
            floatArrayOf(130f, 750f, 3f, 150f),
            floatArrayOf(920f, 190f, 5f, 255f),
            floatArrayOf(840f, 270f, 3f, 190f),
            floatArrayOf(960f, 380f, 4f, 210f),
            floatArrayOf(890f, 510f, 3f, 170f),
            floatArrayOf(940f, 650f, 5f, 240f),
            floatArrayOf(860f, 780f, 3f, 160f),
            floatArrayOf(300f, 160f, 3f, 180f),
            floatArrayOf(760f, 170f, 4f, 200f)
        )

        for (star in starPositions) {
            starPaint.alpha = star[3].toInt()
            canvas.drawCircle(star[0], star[1], star[2], starPaint)
        }

        // Golden 4-point constellation sparkles
        drawSparkle(canvas, 170f, 220f, 32f, Color.parseColor("#FDE047"))
        drawSparkle(canvas, 910f, 240f, 30f, Color.parseColor("#F59E0B"))
        drawSparkle(canvas, 130f, 420f, 26f, Color.parseColor("#FDE047"))
        drawSparkle(canvas, 950f, 450f, 28f, Color.parseColor("#F59E0B"))
    }

    /**
     * Standard text box for Tiers 1 and 2.
     */
    private fun drawStatementBox(
        canvas: Canvas,
        cardRect: RectF,
        statementText: String,
        startY: Float,
        canvasWidth: Int
    ) {
        val statementPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val maxWidth = cardRect.width() - 140f
        val lines = wrapText(statementText, statementPaint, maxWidth)
        var lineY = startY
        val lineHeight = 54f

        for (line in lines) {
            canvas.drawText(line, canvasWidth / 2f, lineY, statementPaint)
            lineY += lineHeight
        }
    }

    /**
     * Dark translucent glassmorphism box for Cosmic Tier 3.
     */
    private fun drawCosmicStatementBox(
        canvas: Canvas,
        cardRect: RectF,
        statementText: String,
        startY: Float,
        canvasWidth: Int
    ) {
        val statementPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 37f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val maxWidth = cardRect.width() - 140f
        val lines = wrapText(statementText, statementPaint, maxWidth)

        // Pill background
        val pillHeight = (lines.size * 54f) + 40f
        val pillRect = RectF(cardRect.left + 40f, startY - 40f, cardRect.right - 40f, startY + pillHeight - 40f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#20FFFFFF")
            style = Paint.Style.FILL
        }
        val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30FDE047")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)
        canvas.drawRoundRect(pillRect, 24f, 24f, pillBorder)

        var lineY = startY + 10f
        val lineHeight = 54f

        for (line in lines) {
            canvas.drawText(line, canvasWidth / 2f, lineY, statementPaint)
            lineY += lineHeight
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "Piggy_Streak_${System.currentTimeMillis()}.png"
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PiggyLedger")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    true
                } else false
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val piggyDir = File(imagesDir, "PiggyLedger").apply { mkdirs() }
                val file = File(piggyDir, filename)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareNativeImage(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.cacheDir, "shared_streak.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserTitle = context.getString(R.string.streak_share_modal_title)
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareToMessages(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.cacheDir, "shared_streak.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val smsIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra("sms_body", context.getString(R.string.streak_share_sms_body))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserTitle = context.getString(R.string.streak_share_messages)
            context.startActivity(Intent.createChooser(smsIntent, chooserTitle))
        } catch (e: Exception) {
            shareNativeImage(context, bitmap)
        }
    }

    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, size: Float, colorInt: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }
        val path = Path()
        path.moveTo(cx, cy - size)
        path.quadTo(cx, cy, cx + size, cy)
        path.quadTo(cx, cy, cx, cy + size)
        path.quadTo(cx, cy, cx - size, cy)
        path.quadTo(cx, cy, cx, cy - size)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun getShareStatement(context: Context, streakCount: Int): String {
        return try {
            val assetName = StreakManager.getStreakMessagesAsset(context)
            val inputStream: InputStream = context.assets.open(assetName)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val categoriesArray = jsonObject.getJSONArray("categories")

            val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val userName = userPrefs.getString("auth_user_name", "")?.takeIf { it.isNotBlank() } ?: "Saver"

            val targetCategoryId = if (streakCount > 0) 8 else 10 // Category 8: Streak Extended / Logged Today, 10: Ghosted/Inactive
            var candidates = mutableListOf<String>()

            for (i in 0 until categoriesArray.length()) {
                val catObj = categoriesArray.getJSONObject(i)
                if (catObj.optInt("id") == targetCategoryId) {
                    val itemsArr = catObj.getJSONArray("items")
                    for (j in 0 until itemsArr.length()) {
                        candidates.add(itemsArr.getString(j))
                    }
                    break
                }
            }

            if (candidates.isEmpty()) {
                if (streakCount > 0) context.getString(R.string.streak_sub_keep_consistency) else context.getString(R.string.streak_sub_start_today)
            } else {
                val selected = candidates[streakCount % candidates.size]
                selected
                    .replace("[Username]", userName)
                    .replace("[USER_NAME]", userName)
                    .replace("[Number]", streakCount.toString())
                    .replace("[Course]", "Budget")
            }
        } catch (e: Exception) {
            if (streakCount > 0) context.getString(R.string.streak_sub_keep_consistency) else context.getString(R.string.streak_sub_start_today)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
