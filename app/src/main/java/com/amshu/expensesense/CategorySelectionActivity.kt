package com.amshu.expensesense

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategorySelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        rvCategories.layoutManager = LinearLayoutManager(this)

        val categories = resources.getStringArray(R.array.transaction_categories).toList()
        
        val adapter = CategoryAdapter(categories) { selectedCategory ->
            val resultIntent = Intent().apply {
                putExtra("selected_category", selectedCategory)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        
        rvCategories.adapter = adapter
    }
}
