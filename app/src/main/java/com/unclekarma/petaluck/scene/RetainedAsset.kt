package com.unclekarma.petaluck.scene

/** Scene-lifetime ownership for a resource shared by disposable rounds. Main-thread confined. */
class RetainedAsset<T>(
    private val isValid: (T) -> Boolean,
    private val pin: (T) -> Unit,
    private val release: (T) -> Unit,
) : AutoCloseable {
    private var asset: T? = null
    val current: T? get() = asset?.takeIf(isValid)

    suspend fun getOrLoad(load: suspend () -> T): T {
        current?.let { return it }
        asset = null
        val loaded = load()
        try {
            pin(loaded)
            check(isValid(loaded)) { "Retained asset became invalid while pinning" }
        } catch (e: Exception) {
            if (isValid(loaded)) release(loaded)
            throw e
        }
        asset = loaded
        return loaded
    }

    override fun close() {
        val previous = asset
        asset = null
        if (previous != null && isValid(previous)) release(previous)
    }
}
