package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Launches [block] in [viewModelScope] and lets it finish even if the scope is cancelled midway.
 * For a save or delete started from a detail screen that closes right after: that screen's view
 * model is cleared with it, which with animations off can land between the database write and the
 * reminder and widget updates that must follow it.
 */
internal fun ViewModel.launchToCompletion(block: suspend CoroutineScope.() -> Unit): Job =
    viewModelScope.launch { withContext(NonCancellable, block) }
