package com.OnTime.ontime.service

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthService(private val context: Context) {
    
    private val auth = FirebaseAuth.getInstance()
    
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }
    
    fun signOut() {
        auth.signOut()
    }
}
