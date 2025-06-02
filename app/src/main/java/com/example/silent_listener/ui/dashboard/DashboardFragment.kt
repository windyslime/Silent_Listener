package com.example.silent_listener.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.silent_listener.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupAudioQualitySettings()
        setupSwitchSettings()
        
        return root
    }
    
    private fun setupAudioQualitySettings() {
        // 获取保存的音频质量设置，默认为高质量
        val savedQuality = sharedPreferences.getString("audio_quality", "high")
        
        // 设置单选按钮组的选中状态
        when (savedQuality) {
            "high" -> binding.radioHigh.isChecked = true
            "medium" -> binding.radioMedium.isChecked = true
            "low" -> binding.radioLow.isChecked = true
            else -> binding.radioHigh.isChecked = true
        }
        
        // 设置单选按钮组的监听器
        binding.radioGroupQuality.setOnCheckedChangeListener { _, checkedId ->
            val quality = when (checkedId) {
                R.id.radioHigh -> "high"
                R.id.radioMedium -> "medium"
                R.id.radioLow -> "low"
                else -> "high"
            }
            
            // 保存设置
            sharedPreferences.edit().putString("audio_quality", quality).apply()
            dashboardViewModel.setAudioQuality(quality)
        }
    }
    
    private fun setupSwitchSettings() {
        // 获取保存的降噪和自动增益控制设置
        val noiseReduction = sharedPreferences.getBoolean("noise_reduction", false)
        val autoGainControl = sharedPreferences.getBoolean("auto_gain_control", true)
        
        // 设置开关的初始状态
        binding.switchNoiseReduction.isChecked = noiseReduction
        binding.switchAutoGain.isChecked = autoGainControl
        
        // 设置降噪开关的监听器
        binding.switchNoiseReduction.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("noise_reduction", isChecked).apply()
            dashboardViewModel.setNoiseReduction(isChecked)
        }
        
        // 设置自动增益控制开关的监听器
        binding.switchAutoGain.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("auto_gain_control", isChecked).apply()
            dashboardViewModel.setAutoGainControl(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}