package com.OnTime.ontime.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.OnTime.ontime.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes

class AuthActivity : AppCompatActivity() {

    private lateinit var googleSignInButton: Button
    private lateinit var browseWithoutLoginButton: Button
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Foreground location access granted. Now request background location.
                requestBackgroundLocationPermission()
            } else {
                // Foreground location access denied.
                Toast.makeText(this, "위치 권한이 거부되었습니다. 앱의 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
                proceedToMain()
            }
        }

    private val requestBackgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Background location access granted.
                Toast.makeText(this, "백그라운드 위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // Background location access denied.
                Toast.makeText(this, "백그라운드 위치 권한이 거부되었습니다. 정확한 알림을 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
            // Proceed to main activity regardless of background permission result
            proceedToMain()
        }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("AuthActivity", "firebaseAuthWithGoogle:" + account.id)
                // Signed in successfully, now request permissions.
                requestPermissionsAndNavigate()
            } catch (e: ApiException) {
                // Google Sign In failed
                Log.w("AuthActivity", "Google sign in failed", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInButton = findViewById(R.id.google_sign_in_button)
        browseWithoutLoginButton = findViewById(R.id.browse_without_login_button)

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        browseWithoutLoginButton.setOnClickListener {
            // Also request permissions when browsing without login
            requestPermissionsAndNavigate()
        }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // User is already signed in, request permissions.
            requestPermissionsAndNavigate()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsAndNavigate() {
        if (allPermissionsGranted()) {
            // If all permissions are already granted, just proceed.
            proceedToMain()
        } else {
            // Request foreground location permissions first.
            requestMultiplePermissionsLauncher.launch(permissions)
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                    .setTitle("백그라운드 위치 권한 필요")
                    .setMessage("이 앱은 정확한 출발 알림을 제공하기 위해 백그라운드에서 사용자의 위치 정보에 접근해야 합니다. '항상 허용'으로 설정해주세요.")
                    .setPositiveButton("설정으로 이동") { _, _ ->
                        // Take user to app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        // DO NOT proceed to main automatically. The user will navigate back manually.
                        // When they return, onStart will be called, re-triggering the permission check.
                    }
                    .setNegativeButton("취소") { _, _ ->
                        Toast.makeText(this, "백그라운드 위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                        proceedToMain()
                    }
                    .create()
                    .show()
            } else {
                proceedToMain()
            }
        } else {
            // For versions below Q, background permission is granted along with foreground permission.
            proceedToMain()
        }
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
