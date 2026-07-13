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
        
        // Reverse wording: keyword "dawa" first (Claude's test case)
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

        // Single amount
        val (medOnly, payOnly) = VoiceIntentParser.extractAmounts("Suresh ko dedh sau ki dawa di")
        assertEquals(150.0, medOnly)
        assertNull(payOnly)
    }
}
