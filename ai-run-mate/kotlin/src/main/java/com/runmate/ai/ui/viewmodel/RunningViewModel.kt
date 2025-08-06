package com.runmate.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.runmate.ai.data.model.RunningSession
import com.runmate.ai.data.repository.RunningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 跑步界面ViewModel
 */
class RunningViewModel(
    private val runningRepository: RunningRepository
) : ViewModel() {
    
    private val _currentSession = MutableStateFlow<RunningSession?>(null)
    val currentSession: StateFlow<RunningSession?> = _currentSession.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadActiveSession()
    }
    
    /**
     * 加载当前活跃的跑步会话
     */
    private fun loadActiveSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val activeSession = runningRepository.getActiveSession()
                _currentSession.value = activeSession
            } catch (e: Exception) {
                // 处理错误
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新当前会话
     */
    fun updateCurrentSession(session: RunningSession) {
        _currentSession.value = session
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        loadActiveSession()
    }
}

/**
 * RunningViewModel工厂类
 */
class RunningViewModelFactory(
    private val runningRepository: RunningRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunningViewModel::class.java)) {
            return RunningViewModel(runningRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}