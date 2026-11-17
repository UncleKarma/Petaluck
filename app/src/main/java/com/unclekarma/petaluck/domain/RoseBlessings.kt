package com.unclekarma.petaluck.domain

import kotlin.random.Random

object RoseBlessings {
    const val COLORFUL_FINALE = "你已经摘尽所有花色，但生活的色彩远不止于此。谢谢你陪我走到这里；接下来的，请替我去发现。"
    val messages = listOf(
        "愿你一直被温柔以待。",
        "愿你心有暖阳，笑意常在。",
        "愿你所遇，皆有小欢喜。",
        "愿你眼里有光，心中有爱。",
        "愿你今天，也有小小欢喜。",
        "愿你被爱，也自在地爱。",
        "愿你每一步，都走向明亮。",
        "愿你心里有花，四季温柔。",
        "愿你的笑容，常被生活珍藏。",
        "愿你慢慢来，好事慢慢开。",
        "愿今天的微风，送来好心情。",
        "愿你与美好，不期而遇。",
    )

    fun sample(random: Random = Random.Default): String = messages[random.nextInt(messages.size)]
}
