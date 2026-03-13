package com.example.es

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton

    // Callback set by HomeFragment so FAB routes through it
    private var addExpenseAction: (() -> Unit)? = null

    fun setAddExpenseLauncher(action: () -> Unit) {
        addExpenseAction = action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabAdd = findViewById(R.id.fabAdd)

        // Load default fragment and apply home nav state
        if (savedInstanceState == null) {
            applyHomeNav()
            loadFragment(HomeFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    applyHomeNav()
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_analytics -> {
                    applyNormalNav()
                    loadFragment(AnalyticsFragment())
                    true
                }
                R.id.nav_wallet -> {
                    applyNormalNav()
                    loadFragment(WalletFragment())
                    true
                }
                R.id.nav_profile -> {
                    applyNormalNav()
                    loadFragment(ProfileFragment())
                    true
                }
                R.id.nav_placeholder -> {
                    // Spacer item — do nothing
                    false
                }
                else -> false
            }
        }

        // FAB delegates to HomeFragment when available, otherwise opens AddExpenseActivity directly
        fabAdd.setOnClickListener {
            addExpenseAction?.invoke()
                ?: startActivity(Intent(this, AddExpenseActivity::class.java))
        }
    }

    /**
     * Home screen:
     * • Switch to 5-item menu (center placeholder creates the visual gap)
     * • Show the FAB floating above the center gap
     */
    private fun applyHomeNav() {
        if (bottomNavigationView.menu.size() != 5) {
            val currentId = bottomNavigationView.selectedItemId
            bottomNavigationView.menu.clear()
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_home)
            // Re-select home after menu swap
            bottomNavigationView.selectedItemId = R.id.nav_home
            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        applyHomeNav(); loadFragment(HomeFragment()); true
                    }
                    R.id.nav_analytics -> {
                        applyNormalNav(); loadFragment(AnalyticsFragment()); true
                    }
                    R.id.nav_wallet -> {
                        applyNormalNav(); loadFragment(WalletFragment()); true
                    }
                    R.id.nav_profile -> {
                        applyNormalNav(); loadFragment(ProfileFragment()); true
                    }
                    R.id.nav_placeholder -> false
                    else -> false
                }
            }
        }
        showFab()
    }

    /**
     * Non-home screens:
     * • Switch to normal 4-item menu (evenly spread)
     * • Hide the FAB
     */
    private fun applyNormalNav() {
        if (bottomNavigationView.menu.size() != 4) {
            bottomNavigationView.menu.clear()
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu)
            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        applyHomeNav(); loadFragment(HomeFragment()); true
                    }
                    R.id.nav_analytics -> {
                        applyNormalNav(); loadFragment(AnalyticsFragment()); true
                    }
                    R.id.nav_wallet -> {
                        applyNormalNav(); loadFragment(WalletFragment()); true
                    }
                    R.id.nav_profile -> {
                        applyNormalNav(); loadFragment(ProfileFragment()); true
                    }
                    else -> false
                }
            }
        }
        hideFab()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_enter,   // new fragment enters
                R.anim.fragment_exit     // old fragment exits
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Expand from center — circle grows outward from a single point.
     * OvershootInterpolator gives a subtle elastic pop at the end.
     */
    private fun showFab() {
        fabAdd.scaleX = 0f
        fabAdd.scaleY = 0f
        fabAdd.visibility = View.VISIBLE
        fabAdd.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()
    }

    /**
     * Shrink to center — circle collapses inward until it vanishes completely.
     */
    private fun hideFab() {
        fabAdd.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(230)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { fabAdd.visibility = View.GONE }
            .start()
    }
}