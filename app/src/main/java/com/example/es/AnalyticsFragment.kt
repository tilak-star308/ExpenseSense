package com.example.es

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.es.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: AnalyticsViewModel
    private val selectedCalendar = Calendar.getInstance()
    private var currentTimeRange = "Day"

    private lateinit var topSpendingAdapter: TopSpendingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
        setupCharts()
        observeData()

        // Initial fetch
        viewModel.fetchData(currentTimeRange, selectedCalendar)
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = TransactionRepository(database.transactionDao())
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[AnalyticsViewModel::class.java]
    }

    private fun setupUI() {
        // Tab Selection
        val tabs = mapOf(
            binding.tabDay to "Day",
            binding.tabWeek to "Week",
            binding.tabMonth to "Month",
            binding.tabYear to "Year"
        )

        tabs.forEach { (view, range) ->
            view.setOnClickListener {
                updateTabUI(view)
                currentTimeRange = range
                viewModel.fetchData(currentTimeRange, selectedCalendar)
            }
        }

        // Spinner Setup
        val types = arrayOf("Expense", "Income")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter

        // Recycler Views
        topSpendingAdapter = TopSpendingAdapter(emptyList())
        binding.rvTopSpending.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTopSpending.adapter = topSpendingAdapter

        // Heatmap Month Spinner
        viewModel.availableMonths.observe(viewLifecycleOwner) { months ->
            val monthStrings = months.map { SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it.time) }
            val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthStrings)
            monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerHeatmapMonth.adapter = monthAdapter
            
            // Set current month as default selection if possible
            val currentIdx = months.indexOfFirst { it.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) && it.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) }
            if (currentIdx != -1) binding.spinnerHeatmapMonth.setSelection(currentIdx)
            
            binding.spinnerHeatmapMonth.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val targetMonth = months[position]
                    selectedCalendar.set(Calendar.MONTH, targetMonth.get(Calendar.MONTH))
                    selectedCalendar.set(Calendar.YEAR, targetMonth.get(Calendar.YEAR))
                    viewModel.fetchData(currentTimeRange, selectedCalendar)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
    }

    private fun updateTabUI(selected: TextView) {
        val allTabs = listOf(binding.tabDay, binding.tabWeek, binding.tabMonth, binding.tabYear)
        allTabs.forEach {
            it.setBackgroundResource(0)
            it.setTextColor(Color.parseColor("#999999"))
            it.typeface = android.graphics.Typeface.DEFAULT
        }
        selected.setBackgroundResource(R.drawable.toggle_selector_active)
        selected.setTextColor(Color.WHITE)
        selected.typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun setupCharts() {
        configureLineChart(binding.lineChart)
        configurePieChart(binding.pieChartCategory, "Category")
        configurePieChart(binding.pieChartAccount, "Account")
    }

    private fun configureLineChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#999999")
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F2F6F6")
                textColor = Color.parseColor("#999999")
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun configurePieChart(chart: PieChart, centerText: String) {
        chart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            this.centerText = centerText
            legend.isEnabled = false
        }
    }

    private var currentHeatmapTotals: List<Float> = emptyList()

    private fun observeData() {
        viewModel.topSpending.observe(viewLifecycleOwner) {
            topSpendingAdapter.updateData(it)
        }

        viewModel.chartData.observe(viewLifecycleOwner) { (entries, labels) ->
            updateLineChart(entries, labels)
        }

        viewModel.filteredTransactions.observe(viewLifecycleOwner) { transactions ->
            updatePieCharts(transactions)
        }

        viewModel.heatmapData.observe(viewLifecycleOwner) {
            updateHeatmapUI(it)
        }

        viewModel.heatmapTotals.observe(viewLifecycleOwner) {
            currentHeatmapTotals = it
        }
    }

    private fun updateLineChart(entries: List<Entry>, labels: List<String>) {
        if (entries.isEmpty()) {
            binding.lineChart.clear()
            return
        }

        binding.lineChart.xAxis.apply {
            valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
            granularity = 1f
            // Increase label count for better detail
            val labelCount = if (labels.size > 20) 12 else labels.size
            setLabelCount(labelCount, false)
            // Rotate labels slightly if they are dense
            labelRotationAngle = if (labels.size > 15) -15f else 0f
        }

        // Set Marker
        val marker = CustomMarkerView(requireContext(), R.layout.layout_chart_marker, labels)
        marker.chartView = binding.lineChart
        binding.lineChart.marker = marker

        val dataSet = LineDataSet(entries, "Expenses").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            color = Color.parseColor("#2ABFBF")
            setCircleColor(Color.parseColor("#2ABFBF"))
            lineWidth = 3f
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.budget_progress_drawable)
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(true)
            highLightColor = Color.parseColor("#2ABFBF")
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.invalidate()
    }

    private fun updatePieCharts(transactions: List<Transaction>) {
        val categoryMap = transactions.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val categoryEntries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }
        
        val accountMap = transactions.groupBy { it.accountName }.mapValues { it.value.sumOf { t -> t.amount } }
        val accountEntries = accountMap.map { PieEntry(it.value.toFloat(), it.key) }

        setPieData(binding.pieChartCategory, categoryEntries)
        setPieData(binding.pieChartAccount, accountEntries)
    }

    private fun setPieData(chart: PieChart, entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }

        // Set Marker
        val marker = CustomMarkerView(requireContext(), R.layout.layout_chart_marker)
        marker.chartView = chart
        chart.marker = marker

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#2ABFBF"),
                Color.parseColor("#F7931A"),
                Color.parseColor("#1B1B1B"),
                Color.parseColor("#AAAAAA")
            )
            sliceSpace = 3f
            setDrawValues(false)
        }
        chart.data = PieData(dataSet)
        chart.invalidate()
    }

    private fun updateHeatmapUI(intensities: List<Float>) {
        binding.heatmapGrid.removeAllViews()
        
        // Calculate the first day of the month offset
        val tempCal = selectedCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val offset = firstDayOfWeek - 1 // 0=Sun, 1=Mon...

        val screenWidth = resources.displayMetrics.widthPixels
        val padding = 40 // Total padding from parent (20dp * 2)
        val cellSize = (screenWidth - (padding * resources.displayMetrics.density).toInt()) / 7
        
        // Add empty offset cells
        for (i in 0 until offset) {
            val emptyView = View(requireContext())
            emptyView.layoutParams = ViewGroup.LayoutParams(cellSize, cellSize)
            binding.heatmapGrid.addView(emptyView)
        }

        // Add date cells
        intensities.forEachIndexed { index, intensity ->
            val dayOfMonth = index + 1
            val cellBinding = com.example.es.databinding.ItemHeatmapCellBinding.inflate(
                LayoutInflater.from(requireContext()), binding.heatmapGrid, false
            )
            
            cellBinding.root.layoutParams = ViewGroup.LayoutParams(cellSize, cellSize)
            cellBinding.tvDate.text = dayOfMonth.toString()
            
            val alpha = (intensity * 255).toInt().coerceAtMost(255).coerceAtLeast(0)
            val overlayAlpha = if (intensity > 0) alpha.coerceAtLeast(30) else 0
            cellBinding.colorOverlay.setBackgroundColor(Color.argb(overlayAlpha, 42, 191, 191))
            
            // Adjust text color based on background intensity for readability
            if (intensity > 0.6) {
                cellBinding.tvDate.setTextColor(Color.WHITE)
            } else {
                cellBinding.tvDate.setTextColor(Color.parseColor("#1A1A2E"))
            }

            cellBinding.root.setOnClickListener {
                val total = if (index < currentHeatmapTotals.size) currentHeatmapTotals[index] else 0f
                showHeatmapMarker(cellBinding.root, dayOfMonth, total)
            }

            binding.heatmapGrid.addView(cellBinding.root)
        }
    }

    private fun showHeatmapMarker(anchorView: View, day: Int, amount: Float) {
        val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_chart_marker, null)
        val tvTitle = markerView.findViewById<TextView>(R.id.tvMarkerTitle)
        val tvValue = markerView.findViewById<TextView>(R.id.tvMarkerValue)

        tvTitle.text = "Day $day"
        tvValue.text = "₹${String.format("%.2f", amount)}"

        val popup = android.widget.PopupWindow(
            markerView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.elevation = 10f
        popup.overlapAnchor = true

        // Show above the anchor
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOffset = (anchorView.width - markerView.measuredWidth) / 2
        // Since overlapAnchor is true, yOffset starts from top of anchor
        val yOffset = -markerView.measuredHeight - 10

        popup.showAsDropDown(anchorView, xOffset, yOffset)

        // Auto-hide after some time
        anchorView.postDelayed({
            if (popup.isShowing) {
                popup.dismiss()
            }
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
