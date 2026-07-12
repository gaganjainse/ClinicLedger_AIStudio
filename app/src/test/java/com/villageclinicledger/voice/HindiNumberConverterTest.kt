package com.villageclinicledger.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HindiNumberConverterTest {

    @Test
    fun testParseHindiNumber_basic() {
        // Devanagari
        assertEquals(520.0, HindiNumberConverter.parseHindiNumber("पाँच सौ बीस"), 0.001)
        assertEquals(150.0, HindiNumberConverter.parseHindiNumber("डेढ़ सौ"), 0.001)
        assertEquals(250.0, HindiNumberConverter.parseHindiNumber("ढाई सौ"), 0.001)
        assertEquals(350000.0, HindiNumberConverter.parseHindiNumber("साढ़े तीन लाख"), 0.001)
        assertEquals(125.0, HindiNumberConverter.parseHindiNumber("सवा सौ"), 0.001)
        assertEquals(75.0, HindiNumberConverter.parseHindiNumber("पौने सौ"), 0.001)

        // Hinglish
        assertEquals(1500.0, HindiNumberConverter.parseHindiNumber("dedh hazaar"), 0.001)
        assertEquals(2500.0, HindiNumberConverter.parseHindiNumber("dhai hazaar"), 0.001)
        assertEquals(50000.0, HindiNumberConverter.parseHindiNumber("pachaas hazaar"), 0.001)

        // Digits fallback
        assertEquals(500.0, HindiNumberConverter.parseHindiNumber("500"), 0.001)
        assertEquals(1234.0, HindiNumberConverter.parseHindiNumber("1234"), 0.001)

        // Mixed
        assertEquals(520.0, HindiNumberConverter.parseHindiNumber("पाँच सौ 20"), 0.001)
    }

    @Test
    fun testParseHindiNumber_edgeCases() {
        // Empty
        assertNull(HindiNumberConverter.parseHindiNumber(""))
        assertNull(HindiNumberConverter.parseHindiNumber("   "))
        assertNull(HindiNumberConverter.parseHindiNumber("रुपये")) // only currency word
        assertNull(HindiNumberConverter.parseHindiNumber("कोई संख्या नहीं"))

        // With currency suffixes (should be stripped)
        assertEquals(500.0, HindiNumberConverter.parseHindiNumber("500 रुपये"), 0.001)
        assertEquals(500.0, HindiNumberConverter.parseHindiNumber("500 rupaye"), 0.001)
        assertEquals(500.0, HindiNumberConverter.parseHindiNumber("500 paise"), 0.001)
        assertEquals(500.0, HindiNumberConverter.parseHindiNumber("500 पैसे"), 0.001)
    }

    @Test
    fun testConvertShort() {
        assertEquals("शून्य", HindiNumberConverter.convertShort(0.0))
        assertEquals("एक", HindiNumberConverter.convertShort(1.0))
        assertEquals("दस", HindiNumberConverter.convertShort(10.0))
        assertEquals("बीस", HindiNumberConverter.convertShort(20.0))
        assertEquals("इक्कीस", HindiNumberConverter.convertShort(21.0))
        assertEquals("एक सौ", HindiNumberConverter.convertShort(100.0))
        assertEquals("एक सौ एक", HindiNumberConverter.convertShort(101.0))
        assertEquals("पाँच सौ बीस", HindiNumberConverter.convertShort(520.0))
        assertEquals("एक हज़ार", HindiNumberConverter.convertShort(1000.0))
        assertEquals("दस हज़ार", HindiNumberConverter.convertShort(10000.0))
        assertEquals("एक लाख", HindiNumberConverter.convertShort(100000.0))
        assertEquals("दस लाख", HindiNumberConverter.convertShort(1000000.0))
        assertEquals("एक करोड़", HindiNumberConverter.convertShort(10000000.0))
    }

    @Test
    fun testConvert_withPaise() {
        // Whole rupees only
        assertEquals("शून्य रुपये", HindiNumberConverter.convert(0.0))
        assertEquals("पाँच सौ बीस रुपये", HindiNumberConverter.convert(520.0))

        // With paise
        assertEquals("एक सौ रुपये 50 पैसे", HindiNumberConverter.convert(100.50))
        assertEquals("पाँच सौ बीस रुपये 25 पैसे", HindiNumberConverter.convert(520.25))
    }

    @Test
    fun testRoundTrip() {
        // Test that parse(convert(x)) ≈ x for a range of values
        val testValues = doubleArrayOf(0.0, 1.0, 1.25, 1.5, 1.75, 2.5, 10.0, 21.0, 520.0, 1000.0, 1500.0, 2500.0, 52000.0, 100000.0)
        for (v in testValues) {
            val words = HindiNumberConverter.convertShort(v)
            val parsed = HindiNumberConverter.parseHindiNumber(words)
            assertNotNull("Failed to parse: $words", parsed)
            assertEquals(v, parsed!!, 1.0, "Round-trip failed for $v → $words → $parsed")
        }
    }
}