package com.example.securityzone

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.content.Context
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var values: FloatArray? = null
    private var labels: Array<String>? = null
    private var maxValue: Int = 0
    private val colors = listOf(
        Color.parseColor("#3498db"), // Azul
        Color.parseColor("#e74c3c"), // Rojo
        Color.parseColor("#f1c40f"), // Amarillo
        Color.parseColor("#2ecc71"), // Verde
        Color.parseColor("#9b59b6"), // Púrpura
        Color.parseColor("#e67e22")  // Naranja
    )

    init {
        paint.color = Color.parseColor("#3498db")
        paint.strokeWidth = 2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun setValues(values: FloatArray, labels: Array<String>, maxValue: Int) {
        this.values = values
        this.labels = labels
        this.maxValue = maxValue
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dibuja el eje X
        paint.color = Color.BLACK
        canvas.drawLine(0f, height.toFloat() - 60f, width.toFloat(), height.toFloat() - 60f, paint)

        // Dibuja las barras
        values?.let { values ->
            labels?.let { labels ->
                for (i in values.indices) {
                    val value = values[i]
                    val barWidth = width.toFloat() / (values.size + 1)
                    val barHeight = (value / maxValue) * (height.toFloat() - 80f)
                    val rectF = RectF((i + 1) * barWidth, height.toFloat() - barHeight - 60f, (i + 2) * barWidth, height.toFloat() - 60f)
                    paint.color = colors[i % colors.size]
                    canvas.drawRoundRect(rectF, 10f, 10f, paint)

                    // Dibuja el valor encima de la barra
                    paint.color = Color.WHITE
                    paint.textSize = 30f
                    val valueText = value.toString()
                    val valueTextWidth = paint.measureText(valueText)
                    canvas.drawText(valueText, rectF.centerX() - valueTextWidth / 2, rectF.top - 10f, paint)

                    // Dibuja la etiqueta de la barra
                    paint.color = Color.WHITE
                    paint.textSize = 20f
                    canvas.save()
                    canvas.rotate(-45f, (i + 1.5f) * barWidth, height.toFloat() - 10f)
                    canvas.drawText(labels[i], (i + 1.5f) * barWidth - paint.measureText(labels[i]) / 2, height.toFloat() - 10f, paint)
                    canvas.restore()
                }
            }
        }

        // Dibuja el título centrado
        paint.color = Color.WHITE
        paint.textSize = 40f
        val title = "Grafico Mensual"
        val titleWidth = paint.measureText(title)
        val xPos = (width - titleWidth) / 2
        canvas.drawText(title, xPos, 50f, paint)
    }
}
