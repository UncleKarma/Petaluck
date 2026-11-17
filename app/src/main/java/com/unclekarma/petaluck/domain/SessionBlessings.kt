package com.unclekarma.petaluck.domain

import kotlin.random.Random

/** One shuffled deck per scene; ordinary wishes are not repeated during a session. */
class SessionBlessings(private val random: Random = Random.Default) {
    private val remaining = RoseBlessings.messages.shuffled(random).toMutableList()

    fun next(): String {
        if (remaining.isEmpty()) remaining += RoseBlessings.messages.shuffled(random)
        return remaining.removeAt(remaining.lastIndex)
    }
}
