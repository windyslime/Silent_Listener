package com.example.silent_listener.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.silent_listener.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioService : Service() {
    companion object {
        private const val TAG = "AudioService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_service_channel"
        
        // 音频配置
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // 服务状态
        var isRunning = false
            private set
        
        // 音量增益
        var volumeGain = 3.0f
            set(value) {
                field = value.coerceIn(1.0f, 5.0f)
            }
    }
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var bufferSize = 0
    private var isProcessing = false
    private var processingJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建")
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 获取唤醒锁，保持CPU运行
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioService::WakeLock")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动")
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        
        // 获取唤醒锁
        wakeLock?.acquire(10*60*1000L /*10分钟*/)
        
        // 启动音频处理
        startAudioProcessing()
        
        isRunning = true
        
        // 如果服务被系统杀死，会自动重启
        return START_STICKY
    }
    
    override fun onDestroy() {
        Log.d(TAG, "服务销毁")
        
        // 停止音频处理
        stopAudioProcessing()
        
        // 释放唤醒锁
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        isRunning = false
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "音频服务"
            val descriptionText = "用于后台放大声音的服务"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("静听正在运行")
            .setContentText("正在放大周围声音")
            .setSmallIcon(R.drawable.ic_play_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun startAudioProcessing() {
        if (isProcessing) return
        
        try {
            // 计算缓冲区大小
            bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT
            )
            
            // 初始化录音器
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                bufferSize
            )
            
            // 初始化播放器
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG_OUT)
                    .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            // 开始录音和播放
            audioRecord?.startRecording()
            audioTrack?.play()
            
            isProcessing = true
            
            // 在协程中处理音频数据
            processingJob = CoroutineScope(Dispatchers.IO).launch {
                processAudio()
            }
            
            Log.d(TAG, "音频处理已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动音频处理失败", e)
            releaseAudioResources()
        }
    }
    
    private fun stopAudioProcessing() {
        if (!isProcessing) return
        
        isProcessing = false
        processingJob?.cancel()
        releaseAudioResources()
        
        Log.d(TAG, "音频处理已停止")
    }
    
    private fun releaseAudioResources() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放音频资源失败", e)
        } finally {
            audioRecord = null
            audioTrack = null
        }
    }
    
    private suspend fun processAudio() {
        val buffer = ShortArray(bufferSize)
        val gainBuffer = ShortArray(bufferSize)
        
        try {
            while (isProcessing) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                
                if (readResult > 0) {
                    // 应用音量增益
                    withContext(Dispatchers.Default) {
                        for (i in 0 until readResult) {
                            // 应用增益并防止溢出
                            val sample = buffer[i] * volumeGain
                            gainBuffer[i] = when {
                                sample > Short.MAX_VALUE -> Short.MAX_VALUE
                                sample < Short.MIN_VALUE -> Short.MIN_VALUE
                                else -> sample.toInt().toShort()
                            }
                        }
                    }
                    
                    // 播放处理后的音频
                    audioTrack?.write(gainBuffer, 0, readResult)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "音频处理过程中出错", e)
        }
    }
}