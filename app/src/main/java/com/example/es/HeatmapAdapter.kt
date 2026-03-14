package com.example.es

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HeatmapAdapter(private var intensityList: List<Float>) :
    RecyclerView.Adapter<HeatmapAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cell: View = view.findViewById(R.id.heatmapCell)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_heatmap_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val intensity = intensityList[position] // 0.0 to 1.0
        
        // Intensity color mapping (Teal shades)
        val baseColor = "#2ABFBF"
        val alpha = (intensity * 255).toInt().coerceAtLeast(20)
        val colorWithAlpha = Color.argb(alpha, 42, 191, 191) // #2ABFBF is 42, 191, 191
        
        holder.cell.setBackgroundColor(colorWithAlpha)
    }

    override fun getItemCount() = intensityList.size

    fun updateData(newList: List<Float>) {
        intensityList = newList
        notifyDataSetChanged()
    }
}
