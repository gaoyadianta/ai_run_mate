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
 * 主界面ViewModel
 */
class MainViewModel(
    private val runningRepository: RunningRepository
) : ViewModel() {
    
    private val _recentSessions = MutableStateFlow<List<RunningSession>>(emptyList())
    val recentSessions: StateFlow<List<RunningSession>> = _recentSessions.asStateFlow()
    
    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()
    
    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // 加载最近的跑步记录
            _recentSessions.value = runningRepository.getRecentCompletedSessions(5)
            
            // 加载统计数据
            _totalDistance.value = runningRepository.getTotalDistance()
            _totalSessions.value = runningRepository.getCompletedSessionsCount()
        }
    }
    
    fun refreshData() {
        loadData()
    }
}

/**
 * MainViewModel工厂类
 */
class MainViewModelFactory(
    private val runningRepository: RunningRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(runningRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}