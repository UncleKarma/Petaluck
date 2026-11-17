package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.RoseBlessings
import com.unclekarma.petaluck.domain.SessionBlessings
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBlessingsTest {
    @Test fun ordinaryWishesDoNotRepeatBeforeDeckIsExhausted() {
        val deck = SessionBlessings(Random(17))
        val wishes = List(RoseBlessings.messages.size) { deck.next() }
        assertEquals(RoseBlessings.messages.toSet(), wishes.toSet())
    }
}
