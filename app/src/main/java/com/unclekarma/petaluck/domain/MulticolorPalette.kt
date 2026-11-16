package com.unclekarma.petaluck.domain

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/** Seeded coloring of the assembled flower's neighboring petals, independent of layout RNG. */
internal object MulticolorPalette {
    fun apply(petals: List<PetalSpec>, seed: Long): List<PetalSpec> {
        val neighbors = List(petals.size) { mutableSetOf<Int>() }
        fun connect(a: Int, b: Int) {
            if (a != b) { neighbors[a].add(b); neighbors[b].add(a) }
        }
        val layers = RoseLayout.layerNames.map { layer ->
            petals.indices.filter { petals[it].variant.substringBefore('_') == layer }
                .sortedBy { normalized(petals[it].angle) }
        }
        layers.forEach { ring ->
            ring.indices.forEach { connect(ring[it], ring[(it + 1) % ring.size]) }
        }
        // Preserve the original generation-order neighbor constraint as well.
        (1 until petals.size).forEach { connect(it - 1, it) }
        for (a in layers.indices) for (b in a + 1 until layers.size) {
            val outer = layers[a]
            val inner = layers[b]
            // Nearby means their angular sectors overlap: half a nominal gap from each layer.
            // Include outer/inner as well as outer/middle and middle/inner, and wrap at 360°.
            val reach = 180f / outer.size + 180f / inner.size
            for (p in outer) for (q in inner) {
                val delta = abs(normalized(petals[p].angle) - normalized(petals[q].angle))
                if (min(delta, 360f - delta) <= reach + .001f) connect(p, q)
            }
        }
        val random = Random(seed xor 0x434F4C4F52L)
        val choices = List(petals.size) { (0 until RoseLayout.BASE_COLOR_COUNT).shuffled(random) }
        val colors = IntArray(petals.size) { -1 }
        // Most constrained first; backtrack rather than ever relaxing a neighbor constraint.
        fun assign(remaining: Int): Boolean {
            if (remaining == 0) return true
            val next = colors.indices.filter { colors[it] < 0 }.maxWithOrNull(
                compareBy<Int> { p -> neighbors[p].map { colors[it] }.filter { it >= 0 }.distinct().size }
                    .thenBy { neighbors[it].size }
            ) ?: return false
            for (color in choices[next]) {
                if (neighbors[next].any { colors[it] == color }) continue
                colors[next] = color
                if (assign(remaining - 1)) return true
                colors[next] = -1
            }
            return false
        }
        check(assign(petals.size)) { "Cannot color the petal neighborhood with the existing palette" }
        return petals.mapIndexed { index, petal -> petal.copy(colorId = colors[index]) }
    }

    private fun normalized(angle: Float): Float = ((angle % 360f) + 360f) % 360f
}
