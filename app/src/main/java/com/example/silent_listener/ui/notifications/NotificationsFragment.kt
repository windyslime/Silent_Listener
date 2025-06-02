package com.example.silent_listener.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.silent_listener.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupAboutInfo()
        setupVersionInfo()
        setupPermissionInfo()
        
        return root
    }
    
    private fun setupAboutInfo() {
        // 设置应用描述
        binding.appDescription.text = getString(R.string.about_app)
    }
    
    private fun setupVersionInfo() {
        // 获取应用版本信息
        val packageInfo = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        
        // 设置版本信息
        val versionName = packageInfo?.versionName ?: getString(R.string.version_number)
        binding.versionValue.text = versionName
    }
    
    private fun setupPermissionInfo() {
        // 设置权限说明
        binding.permissionsDescription.text = getString(R.string.permissions_description)
        
        // 设置开发者信息
        binding.developerValue.text = getString(R.string.developer_name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}