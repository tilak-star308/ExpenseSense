package com.amshu.expensesense

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())
    private val verificationCheckInterval = 4000L // 4 seconds

    private val checkVerificationRunnable = object : Runnable {
        override fun run() {
            checkEmailVerification()
            handler.postDelayed(this, verificationCheckInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verification)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val btnResendEmail = findViewById<MaterialButton>(R.id.btnResendEmail)

        btnResendEmail.setOnClickListener {
            val user = auth.currentUser
            user?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent again.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start checking when activity becomes visible
        handler.post(checkVerificationRunnable)
    }

    override fun onStop() {
        super.onStop()
        // Stop checking when activity is no longer visible to prevent memory leaks and unnecessary checks
        handler.removeCallbacks(checkVerificationRunnable)
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    // Stop checking
                    handler.removeCallbacks(checkVerificationRunnable)
                    
                    Toast.makeText(this, "Email verified successfully", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to Onboarding Activity
                    val intent = Intent(this, Onboarding::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Failed to check verification status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
