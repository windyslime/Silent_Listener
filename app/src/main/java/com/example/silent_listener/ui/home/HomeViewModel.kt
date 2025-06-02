package com.example.silent_listener.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    
    // 音量增益控制，范围1.0-5.0，默认值设为3.0
    private val _volumeGain = MutableLiveData<Float>().apply {
        value = 3.0f
    }
    val volumeGain: LiveData<Float> = _volumeGain
    
    // 设置音量增益
    fun setVolumeGain(gain: Float) {
        _volumeGain.value = gain.coerceIn(1.0f, 5.0f)
    }
}