package com.bayg.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    var profile by mutableStateOf<ProfileUi?>(null)
        private set

    var signedOut by mutableStateOf(false)
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            profile = repository.getProfile()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            signedOut = true
        }
    }
}
