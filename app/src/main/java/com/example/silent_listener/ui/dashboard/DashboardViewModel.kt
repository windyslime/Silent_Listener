package com.example.silent_listener.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    // 音频质量设置
    private val _audioQuality = MutableLiveData<String>().apply {
        value = "high" // 默认高质量
    }
    val audioQuality: LiveData<String> = _audioQuality
    
    // 降噪设置
    private val _noiseReduction = MutableLiveData<Boolean>().apply {
        value = false // 默认关闭
    }
    val noiseReduction: LiveData<Boolean> = _noiseReduction
    
    // 自动增益控制设置
    private val _autoGainControl = MutableLiveData<Boolean>().apply {
        value = true // 默认开启
    }
    val autoGainControl: LiveData<Boolean> = _autoGainControl
    
    // 设置音频质量
    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
    }
    
    // 设置降噪
    fun setNoiseReduction(enabled: Boolean) {
        _noiseReduction.value = enabled
    }
    
    // 设置自动增益控制
    fun setAutoGainControl(enabled: Boolean) {
        _autoGainControl.value = enabled
    }
}