package com.bayg.ui.viewmodel

import com.bayg.Park

sealed interface NearestParkUiState {
    data object Loading : NearestParkUiState
    data class Success(val park: Park, val totalCount: Int) : NearestParkUiState
    data class Error(val message: String) : NearestParkUiState
}