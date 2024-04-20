package com.sergey.nes.recorder.ui


interface ContentUIState<T, PROPS> {
    fun extractParams(): PROPS

    fun contentValue(): UiLCEState.Content<T>
}

sealed class UiLCEState<out T> {
    data object Initial : UiLCEState<Nothing>()
    data object Loading : UiLCEState<Nothing>()
    data class Content<T>(val data: T) : UiLCEState<T>()
    data object Error : UiLCEState<Nothing>()
}

fun <T> UiLCEState.Content<T>.resolve(): T {
    return this.data
}
