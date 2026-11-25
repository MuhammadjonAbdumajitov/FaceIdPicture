package com.example

import com.example.data.local.Converters
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class ExampleUnitTest {

    @Test
    fun convertersRoundTripEmbeddingArray() {
        val converters = Converters()
        val original = floatArrayOf(0.125f, -0.5f, 0.875f, 0.0f)
        val encoded = converters.fromFloatArray(original)
        val decoded = converters.toFloatArray(encoded)
        assertArrayEquals(original, decoded, 1e-6f)
    }

    @Test
    fun cosineSimilarityComputesExpectedScore() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(1f, 0f, 0f)
        val c = floatArrayOf(0f, 1f, 0f)
        assertEquals(1f, cosine(a, b), 1e-5f)
        assertEquals(0f, cosine(a, c), 1e-5f)
    }

    private fun cosine(first: FloatArray, second: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in first.indices) {
            dot += first[i] * second[i]
            normA += first[i] * first[i]
            normB += second[i] * second[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }
}
