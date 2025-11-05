package com.OnTime.ontime.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.OnTime.ontime.R

class AuthActivity : AppCompatActivity() {
    
    private lateinit var googleSignInButton: Button
    private lateinit var browseWithoutLoginButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        googleSignInButton = findViewById(R.id.google_sign_in_button)
        browseWithoutLoginButton = findViewById(R.id.browse_without_login_button)
        
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
        
        browseWithoutLoginButton.setOnClickListener {
            navigateToMain()
        }
    }
    
    private fun signInWithGoogle() {
        // TODO: Implement Google Sign-In with Firebase Authentication
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
