package com.example.flowertears.p18ab52cf.scene

import kotlinx.coroutines.CancellationException

/** Native audio failure cannot propagate into flower assembly; stale work still cancels. */
suspend fun prepareOptionalAudio(prepare: suspend () -> Unit, onFailure: (Exception) -> Unit): Boolean {
    return try {
        prepare()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFailure(e)
        false
    }
}
