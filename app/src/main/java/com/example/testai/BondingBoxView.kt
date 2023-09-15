package com.example.testai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BoundingBoxView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val boundingBoxPaint = Paint()
    private val textViewPaint = Paint()
    private var x1: Float = 0f
    private var y1: Float = 0f
    private var x2: Float = 0f
    private var y2: Float = 0f
    private var label : String = ""

    init {
        boundingBoxPaint.color = Color.RED // Warna bounding box (misalnya merah)
        boundingBoxPaint.style = Paint.Style.STROKE
        boundingBoxPaint.strokeWidth = 4.0f
        textViewPaint.textSize = 40.toFloat()
        textViewPaint.color = Color.WHITE
        boundingBoxPaint.color
    // Ketebalan garis bounding box
    }

    // Fungsi untuk mengatur koordinat bounding box
    fun setBoundingBox(x1: Float, y1: Float, x2: Float, y2: Float,label:String) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
        this.label = label
        invalidate() // Meminta tampilan untuk diperbarui
    }

    // Fungsi untuk menggambar bounding box
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(x1, y1, x2, y2, boundingBoxPaint)
        canvas?.drawText("$label ", x1 , y1 -  10, textViewPaint)
    }

}
