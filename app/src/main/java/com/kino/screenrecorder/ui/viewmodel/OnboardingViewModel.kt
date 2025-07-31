package com.kino.screenrecorder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kino.screenrecorder.KinoApplication
import com.kino.screenrecorder.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository: SettingsRepository = 
        (application as KinoApplication).settingsRepository
    
    val isFirstLaunch = settingsRepository.isFirstLaunch
    
    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setFirstLaunchCompleted()
        }
    }
}