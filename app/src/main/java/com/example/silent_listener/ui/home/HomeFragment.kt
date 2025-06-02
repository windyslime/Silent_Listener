package com.example.silent_listener.ui.home

import android.content.Intent
import android.os.Build

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.silent_listener.R
import com.example.silent_listener.databinding.FragmentHomeBinding
import com.example.silent_listener.service.AudioService
import com.google.android.material.slider.Slider

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    
    // 音频处理状态
    private var serviceRunning = false
    
    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startAudioService()
        } else {
            Toast.makeText(requireContext(), "需要麦克风权限才能使用此功能", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        setupUI()
        observeViewModel()
        
        // 自动检查权限并启动服务
        checkPermissionAndStartService()
        
        return root
    }
    
    private fun setupUI() {
        // 设置音量滑块
        binding.sliderVolume.addOnChangeListener { _, value, _ ->
            // 更新服务中的音量增益
            AudioService.volumeGain = value
            homeViewModel.setVolumeGain(value)
        }
        
        // 设置默认音量增益为3.0
        binding.sliderVolume.value = 3.0f
        homeViewModel.setVolumeGain(3.0f)
        
        // 更新UI状态
        updateUIState()
    }
    
    private fun observeViewModel() {
        homeViewModel.volumeGain.observe(viewLifecycleOwner) { gain ->
            binding.sliderVolume.value = gain
            AudioService.volumeGain = gain
        }
    }
    
    private fun updateUIState() {
        // 显示服务状态
        val isRunning = AudioService.isRunning
        serviceRunning = isRunning
        
        binding.statusText.text = if (isRunning) {
            getString(R.string.listening_status, getString(R.string.status_active))
        } else {
            getString(R.string.listening_status, getString(R.string.status_inactive))
        }
    }
    
    private fun checkPermissionAndStartService() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == 
                    PackageManager.PERMISSION_GRANTED -> {
                startAudioService()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(requireContext(), "需要麦克风权限才能使用此功能", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun startAudioService() {
        // 如果服务已经在运行，则不需要再次启动
        if (AudioService.isRunning) {
            updateUIState()
            return
        }
        
        try {
            // 创建并启动前台服务
            val serviceIntent = Intent(requireContext(), AudioService::class.java)
            
            // 在Android 8.0及以上版本，需要使用startForegroundService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
            
            // 更新UI状态
            updateUIState()
            
            Toast.makeText(requireContext(), "声音放大服务已启动", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "启动服务失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}