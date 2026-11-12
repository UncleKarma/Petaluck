package com.example.flowertears.p18ab52cf.scene

/** A delayed attachment can appear after initial composition or be replaced by its host. */
class AttachmentBinding<T : Any>(private val isValid: (T) -> Boolean) {
    private var bound: T? = null

    fun bind(candidate: T?, configure: (T) -> Unit): T? {
        if (candidate == null || !isValid(candidate)) return null
        if (candidate !== bound) {
            configure(candidate)
            bound = candidate
        }
        return candidate
    }

    fun clear() { bound = null }
}
