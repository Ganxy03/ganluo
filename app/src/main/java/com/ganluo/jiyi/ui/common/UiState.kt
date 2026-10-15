package com.ganluo.jiyi.ui.common

/**
 * 通用 UI 状态,业务层不需要写 sealed 重复样板。
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
