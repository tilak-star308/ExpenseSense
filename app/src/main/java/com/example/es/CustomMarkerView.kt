package com.example.es

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.data.PieEntry

class CustomMarkerView(context: Context, layoutResource: Int, private val labels: List<String> = emptyList()) :
    MarkerView(context, layoutResource) {

    private val tvTitle: TextView = findViewById(R.id.tvMarkerTitle)
    private val tvValue: TextView = findViewById(R.id.tvMarkerValue)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is PieEntry) {
            tvTitle.text = e.label
            tvValue.text = "₹${String.format("%.2f", e.value)}"
        } else if (e != null) {
            val index = e.x.toInt()
            if (index in labels.indices && labels[index].isNotEmpty()) {
                tvTitle.text = labels[index]
            } else {
                tvTitle.text = "Value"
            }
            tvValue.text = "₹${String.format("%.2f", e.y)}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat() - 20f)
    }
}
