package com.unclekarma.petaluck.scene

/** Main-thread scene cache. Call retain only after all previous round owners are cleared. */
class RoundAssetSet<T>(
    private val isValid: (T) -> Boolean,
    private val pin: (T) -> Unit,
    private val release: (T) -> Unit,
) : AutoCloseable {
    private val assets = mutableMapOf<String, RetainedAsset<T>>()

    suspend fun retain(paths: Set<String>, load: suspend (String) -> T) {
        for (stale in assets.keys.toList().filter { it !in paths }) assets.remove(stale)?.close()
        for (path in paths) {
            assets.getOrPut(path) { RetainedAsset(isValid, pin, release) }.getOrLoad { load(path) }
        }
    }

    operator fun get(path: String): T? = assets[path]?.current

    override fun close() {
        assets.values.forEach { it.close() }
        assets.clear()
    }
}
