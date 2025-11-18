package com.OnTime.ontime.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OnTime.ontime.data.models.CustomLocation
import com.OnTime.ontime.data.repositories.SettingsRepository
import kotlinx.coroutines.launch

class CustomLocationViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _customLocations = MutableLiveData<List<CustomLocation>>()
    val customLocations: LiveData<List<CustomLocation>> = _customLocations

    private val _saveError = MutableLiveData<String?>(null)
    val saveError: LiveData<String?> = _saveError

    init {
        loadCustomLocations()
    }

    fun loadCustomLocations() {
        viewModelScope.launch {
            _customLocations.value = settingsRepository.getCustomLocations()
        }
    }

    fun saveCustomLocation(location: CustomLocation) {
        viewModelScope.launch {
            when (val result = settingsRepository.saveCustomLocation(location)) {
                is SettingsRepository.SaveResult.Success -> loadCustomLocations()
                is SettingsRepository.SaveResult.UserNotLoggedIn -> _saveError.value = "로그인 상태가 아닙니다. 앱을 다시 시작하거나 다시 로그인해주세요."
                is SettingsRepository.SaveResult.FirestoreError -> _saveError.value = "데이터베이스 저장에 실패했습니다: ${result.message}"
            }
        }
    }

    fun onSaveErrorShown() {
        _saveError.value = null
    }
}
