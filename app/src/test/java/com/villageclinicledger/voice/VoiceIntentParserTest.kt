package com.villageclinicledger.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceIntentParserTest {

    @Test
    fun testExtractPatientName_withEnglishPhrases() {
        // Standard wording
        assertEquals("Ramesh", VoiceIntentParser.extractPatientName("Ramesh ko dawa di 200"))

        // Reverse wording: keyword "dawa" first
        assertEquals("Ramesh", VoiceIntentParser.extractPatientName("dawa di Ramesh 200"))

        // Keyword "medicine" first
        assertEquals("Suresh", VoiceIntentParser.extractPatientName("medicine for Suresh 500"))

        // Keyword "tablet" first
        assertEquals("Mahesh", VoiceIntentParser.extractPatientName("tablet for Mahesh"))
    }

    @Test
    fun testExtractPatientName_withHindiPhrases() {
        // Standard wording in Devanagari Hindi
        assertEquals("रमेश", VoiceIntentParser.extractPatientName("रमेश को दवा दी २००"))

        // Reverse wording: keyword "दवा" first
        assertEquals("रमेश", VoiceIntentParser.extractPatientName("दवा दी रमेश २००"))

        // "बकाया" first
        assertEquals("सतीश", VoiceIntentParser.extractPatientName("बकाया बताओ सतीश का"))
    }

    @Test
    fun testExtractVillageName() {
        // English village name extraction
        assertEquals("Siras", VoiceIntentParser.extractVillageName("Ramesh of Siras"))

        // Transliterated lower-case village name extraction
        assertEquals("Jhilai", VoiceIntentParser.extractVillageName("ramesh of jhilai"))
    }

    @Test
    fun testExtractAmounts_withEnglishAndHindiNumbers() {
        // Double amounts (medicine and payment)
        val (med, pay) = VoiceIntentParser.extractAmounts("Ramesh ko 500 ki dawa di aur 200 jama kiye")
        assertEquals(500.0, med)
        assertEquals(200.0, pay)

        // Single amount (medicine, no payment trigger)
        val (medOnly, payOnly) = VoiceIntentParser.extractAmounts("Suresh ko dedh sau ki dawa di")
        assertEquals(150.0, medOnly)
        assertNull(payOnly)
    }

    @Test
    fun testExtractAmounts_singleAmountWithPaymentTrigger() {
        // Only payment trigger word + single digit amount → payment, not medicine
        val (med, pay) = VoiceIntentParser.extractAmounts("ramesh ko 200 jama kiye")
        assertNull(med)
        assertEquals(200.0, pay)

        // "diye" trigger
        val (med2, pay2) = VoiceIntentParser.extractAmounts("500 diye")
        assertNull(med2)
        assertEquals(500.0, pay2)

        // "diya" trigger
        val (med3, pay3) = VoiceIntentParser.extractAmounts("100 diya")
        assertNull(med3)
        assertEquals(100.0, pay3)
    }

    @Test
    fun testExtractAmounts_noPaymentTrigger() {
        // Two amounts, no payment trigger → first=medicine, second=payment
        val (med, pay) = VoiceIntentParser.extractAmounts("100 and 200")
        assertEquals(100.0, med)
        assertEquals(200.0, pay)

        // Single amount, no trigger → medicine
        val (med2, pay2) = VoiceIntentParser.extractAmounts("just 150")
        assertEquals(150.0, med2)
        assertNull(pay2)
    }

    @Test
    fun testExtractAmounts_emptyAndEdgeCases() {
        // Empty string
        val (med, pay) = VoiceIntentParser.extractAmounts("")
        assertNull(med)
        assertNull(pay)

        // Hindi number + payment trigger
        val (med2, pay2) = VoiceIntentParser.extractAmounts("ramesh ne dedh sau jama kiye")
        assertNull(med2)
        assertEquals(150.0, pay2)
    }

    @Test
    fun testExtractAmounts_withIntent() {
        // PAYMENT intent with single amount → payment
        val (med, pay) = VoiceIntentParser.extractAmounts("Ramesh se 300 mila", IntentType.PAYMENT)
        assertNull(med)
        assertEquals(300.0, pay)

        // MEDICINE intent with single amount → medicine
        val (med2, pay2) = VoiceIntentParser.extractAmounts("Ramesh ko 400 ki dawa di", IntentType.MEDICINE)
        assertEquals(400.0, med2)
        assertNull(pay2)
    }
}