package com.example.flowertears.p18ab52cf.domain

import kotlin.math.*
import kotlin.random.Random

enum class Phase { Loading, Ready, Playing, FinalPetalHeld, Completing, Completed, Error }
enum class PetalState { Attached, Pulling, DetachedHeld, Falling, Removed }
data class Vec(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(v: Vec) = Vec(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec) = Vec(x - v.x, y - v.y, z - v.z)
    operator fun times(f: Float) = Vec(x * f, y * f, z * f)
    fun length() = sqrt(x*x + y*y + z*z)
}
data class PetalSpec(
    val id: Int, val variant: String, val position: Vec, val angle: Float, val tilt: Float, val roll: Float, val scale: Float,
    val uvIndex: Int, val albedoScale: Float, val colorId: Int = 0
)
data class PetalCupProfile(val radius: Float, val height: Float, val tilt: Float)
data class RoundConfig(val seed: Long, val count: Int, val colorId: Int, val petals: List<PetalSpec>) {
    fun validate(available: Set<String> = RoseLayout.variantIds) {
        require(count in 17..30 && colorId in 0 until RoseLayout.COLOR_COUNT)
        require(petals.size == count && petals.map { it.id }.toSet().size == count)
        require(petals.all { it.variant in available && it.position.length().isFinite() && it.scale > 0 })
        require(petals.all { it.colorId in 0 until RoseLayout.BASE_COLOR_COUNT })
        require(petals.all { it.uvIndex in 0..1 && it.albedoScale in .86f..1f })
    }
}
object RoseLayout {
    const val VERSION = "interleaved-petals-v6"
    const val BASE_COLOR_COUNT = 8
    const val MULTICOLOR = BASE_COLOR_COUNT
    const val COLOR_COUNT = BASE_COLOR_COUNT + 1
    const val WHORL_PHASE_JITTER = 20f
    const val ANGLE_JITTER_FRACTION = .09f
    const val WHORL_TILT_JITTER = .075f
    const val PETAL_TILT_JITTER = .025f
    const val WHORL_SCALE_JITTER = .0015f
    const val PETAL_SCALE_JITTER = .0025f
    const val ROLL_JITTER = .02f
    // Blossom-local +Z is the growth axis: 55 degrees above the viewer-facing plane.
    const val HEAD_PITCH = -55f
    // Core meshes grow along +Y; petals use blossom-local +Z. Both share CrownMotion.
    const val CORE_LOCAL_PITCH = 90f
    const val CORE_PITCH = HEAD_PITCH + CORE_LOCAL_PITCH
    val layerNames = listOf("outer", "middle", "inner")
    val cupProfiles = mapOf(
        "outer_01" to PetalCupProfile(.0074f, -.0090f, 18f),
        "outer_02" to PetalCupProfile(.0080f, -.0078f, 24f),
        "outer_03" to PetalCupProfile(.0086f, -.0065f, 30f),
        "outer_04" to PetalCupProfile(.0092f, -.0052f, 42f),
        "middle_01" to PetalCupProfile(.0089f, -.0058f, 36f),
        "middle_02" to PetalCupProfile(.0096f, -.0044f, 48f),
        "middle_03" to PetalCupProfile(.0100f, -.0034f, 54f),
        "middle_04" to PetalCupProfile(.0104f, -.0026f, 66f),
        "inner_01" to PetalCupProfile(.0102f, -.0030f, 60f),
        "inner_02" to PetalCupProfile(.0106f, -.0022f, 72f),
        "inner_03" to PetalCupProfile(.0108f, -.0016f, 78f),
        "inner_04" to PetalCupProfile(.0109f, -.0014f, 82f),
    )
    val variantIds = cupProfiles.keys
    fun texturePath(colorId: Int, variant: String): String {
        require(colorId in 0 until BASE_COLOR_COUNT && variant in variantIds)
        return "rose/petal-$colorId-${variant.substringBefore('_')}.png"
    }
    fun texturePaths(colorId: Int): Set<String> = layerNames.map { texturePath(colorId, "${it}_01") }.toSet()
    fun texturePath(petal: PetalSpec): String = texturePath(petal.colorId, petal.variant)
    fun texturePaths(config: RoundConfig): Set<String> = config.petals.map(::texturePath).toSet()
    fun rainTexturePaths(config: RoundConfig): List<String> =
        if (config.colorId == MULTICOLOR) (0 until BASE_COLOR_COUNT)
            .shuffled(Random(config.seed xor 0x5241494EL))
            .mapIndexed { i, color -> texturePath(color, "${layerNames[i % 3]}_01") }
        else layerNames.map { texturePath(config.colorId, "${it}_01") }
    fun sample(seed: Long, random: Random = Random(seed), excludedColors: Set<Int> = emptySet()): RoundConfig {
        val colors = (0 until COLOR_COUNT).filterNot(excludedColors::contains)
        require(colors.isNotEmpty())
        // Sample count independently and choose uniformly from the eligible colors.
        val n = 17 + random.nextInt(14)
        val color = colors[random.nextInt(colors.size)]
        return assemble(seed, n, color, random)
    }
    fun assemble(seed: Long, n: Int, color: Int, random: Random = Random(seed)): RoundConfig {
        require(n in 17..30 && color in 0 until COLOR_COUNT)
        val outer = ceil(n * .43).toInt()
        val middle = ceil((n - outer) * .58).toInt()
        val counts = listOf(outer, middle, n - outer - middle)
        val petals = mutableListOf<PetalSpec>()
        // Appearance uses its own seeded stream so the layout and round draws stay stable.
        val appearance = Random(seed xor 0x4F89_25C3_5D37_82E1L)
        val placement = Random(seed xor 0x3D82_E16A_79C4_05B7L)
        counts.forEachIndexed { layer, count ->
            val prefix = layerNames[layer]
            val phase = layer * 137.508f + (placement.nextFloat()*2-1)*WHORL_PHASE_JITTER
            val lift = (placement.nextFloat()*2-1)*WHORL_TILT_JITTER
            val ringScale = 1f + (placement.nextFloat()*2-1)*WHORL_SCALE_JITTER
            val shadeRanks = (0 until count).shuffled(appearance)
            val uvChannels = List(count) { it % 2 }.shuffled(appearance)
            val variants = mutableListOf<Int>()
            while (variants.size < count) {
                val bag = (1..4).shuffled(random).toMutableList()
                if (variants.lastOrNull() == bag.first()) java.util.Collections.swap(bag, 0, 1)
                variants += bag
            }
            if (variants[count-1] == variants[0]) {
                val j = (1 until count-1).firstOrNull { variants[it] != variants[0] && variants[it-1] != variants[0] && variants[it+1] != variants[0] && variants[it] != variants[count-2] }
                if (j != null) java.util.Collections.swap(variants, j, count-1)
            }
            repeat(count) { i ->
                val gap = 360f / count
                val angle = i*gap + phase + (random.nextFloat()*2-1)*gap*ANGLE_JITTER_FRACTION
                val a = angle * PI.toFloat() / 180
                val variant = "${prefix}_0${variants[i]}"
                // Adjacent layer families overlap in opening and attachment height.
                val profile = cupProfiles.getValue(variant)
                val radius = profile.radius
                val tilt = profile.tilt + lift + (random.nextFloat()*2-1)*PETAL_TILT_JITTER
                val roll = (appearance.nextFloat()*2-1)*ROLL_JITTER
                val scale = ringScale * (1f + (random.nextFloat()*2-1)*PETAL_SCALE_JITTER)
                petals += PetalSpec(petals.size, variant,
                    Vec(-sin(a)*radius, cos(a)*radius, profile.height), angle,
                    tilt, roll, scale, uvChannels[i],
                    .86f + .14f * (shadeRanks[i] + .5f) / count, if (color == MULTICOLOR) 0 else color)
            }
        }
        val colored = if (color == MULTICOLOR) MulticolorPalette.apply(petals, seed) else petals
        return RoundConfig(seed, n, color, colored).also { it.validate() }
    }
}

/** The last three flowers that became playable in this scene, oldest first. */
class RecentRoseColors {
    private var colors = emptyList<Int>()

    fun sample(seed: Long): RoundConfig = RoseLayout.sample(seed, excludedColors = colors.toSet())

    fun recordReady(colorId: Int) {
        // Retrying the same round after a runtime error should not advance the window.
        if (colors.lastOrNull() == colorId) return
        colors = (colors + colorId).takeLast(3)
    }
}

/** Single authority for result/count. Rendering and callbacks never increment the count. */
class RoseSession(val id: Long, val config: RoundConfig, private val pickBlessing: () -> String = { RoseBlessings.sample() }) {
    private val states = config.petals.associate { it.id to PetalState.Attached }.toMutableMap()
    var phase = Phase.Loading; private set
    var detachedCount = 0; private set
    var paused = false; private set
    var heldId: Int? = null; private set
    var handId: Long? = null; private set
    var blessing: String = ""; private set
    private var finalPetalId: Int? = null
    val result: String get() = if (detachedCount == 0) "" else if ((config.count - detachedCount) % 2 == 0) "喜欢" else "不喜欢"
    fun ready() { config.validate(); if (phase == Phase.Loading) phase = Phase.Ready }
    fun state(petal: Int) = states[petal]
    fun grab(sessionId: Long, petal: Int, hand: Long): Boolean {
        if (sessionId != id || paused || phase !in listOf(Phase.Ready, Phase.Playing) || heldId != null || states[petal] != PetalState.Attached) return false
        heldId = petal; handId = hand; states[petal] = PetalState.Pulling; return true
    }
    fun detach(sessionId: Long, petal: Int, hand: Long): Boolean {
        if (sessionId != id || paused || heldId != petal || handId != hand || states[petal] != PetalState.Pulling) return false
        states[petal] = PetalState.DetachedHeld
        detachedCount++
        phase = if (detachedCount == config.count) Phase.FinalPetalHeld else Phase.Playing
        return true
    }
    fun release(sessionId: Long, hand: Long): PetalState? {
        if (sessionId != id || handId != hand) return null
        val petal = heldId ?: return null
        val next = if (states[petal] == PetalState.DetachedHeld) PetalState.Falling else PetalState.Attached
        states[petal] = next; heldId = null; handId = null
        if (phase == Phase.FinalPetalHeld) {
            blessing = pickBlessing()
            finalPetalId = petal
            phase = Phase.Completing
        }
        return next
    }
    fun removed(petal: Int) {
        if (states[petal] != PetalState.Falling) return
        states[petal] = PetalState.Removed
        if (phase == Phase.Completing && petal == finalPetalId) phase = Phase.Completed
    }
    fun pause() { paused = true }
    fun resume() { paused = false }
    fun fail() { phase = Phase.Error }
}

object PullRules {
    const val DEAD_ZONE = .008f
    const val THRESHOLD = .04f
    /** Distance from the grab origin in meters, independent of the pull direction. */
    fun distance(delta: Vec): Float = delta.length()
    fun strength(distance: Float): Float =
        ((distance - DEAD_ZONE) / (THRESHOLD - DEAD_ZONE)).coerceIn(0f, 1f)
}
