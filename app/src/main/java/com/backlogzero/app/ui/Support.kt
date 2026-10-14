package com.backlogzero.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.backlogzero.app.AppContainer
import com.backlogzero.app.BacklogZeroApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    noinline create: (AppContainer) -> VM,
): VM {
    val app = LocalContext.current.applicationContext as BacklogZeroApp
    return viewModel(key = key, factory = viewModelFactory { initializer { create(app.container) } })
}

/** Laad-, klaar- en foutstatus van een gegevensbron. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String?) : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

/** Zet een bron om in een [Load]-status met opnieuw-proberen-mogelijkheid. */
@OptIn(ExperimentalCoroutinesApi::class)
class Loader<T>(scope: CoroutineScope, source: () -> Flow<T>) {
    private val trigger = MutableStateFlow(0)

    val state: StateFlow<Load<T>> = trigger
        .flatMapLatest {
            source()
                .map<T, Load<T>> { Load.Ready(it) }
                .onStart { emit(Load.Loading) }
                .catch { emit(Load.Failed(it.message)) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), Load.Loading)

    fun retry() {
        trigger.value++
    }
}

fun <T> ViewModel.loader(source: () -> Flow<T>) = Loader(viewModelScope, source)
