package com.OnTime.ontime.ui.screen.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.OnTime.ontime.ui.MainActivity
import com.OnTime.ontime.ui.theme.OnTimeTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                android.util.Log.e("SignIn", "Google Sign-In ApiException after RESULT_OK", e)
                if (e.statusCode == 10) { // CommonStatusCodes.DEVELOPER_ERROR
                    Toast.makeText(this, "Google 로그인 설정 오류. SHA-1 지문 또는 클라이언트 ID를 확인하세요.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Google 계정 정보 처리 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // This block will now handle failures like RESULT_CANCELED
            android.util.Log.w("SignIn", "Google Sign-In failed with resultCode: ${result.resultCode}")
            Toast.makeText(this, "Google 로그인이 취소되었거나 실패했습니다. (코드: ${result.resultCode})", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initGoogleSignIn()
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            OnTimeTheme {
                LoginScreen(
                    onGoogleSignInClick = { signInWithGoogle() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToMain()
        }
    }

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.OnTime.ontime.R.string.default_web_client_id))
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        android.util.Log.d("SignIn", "signInWithGoogle() called.")
        try {
            val signInIntent = googleSignInClient.signInIntent
            android.util.Log.d("SignIn", "Got signInIntent. Launching launcher.")
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            android.util.Log.e("SignIn", "Error during signInWithGoogle", e)
            Toast.makeText(this, "Google 로그인 시작 중 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Firebase 로그인 성공: ${user?.email}", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Firebase 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
