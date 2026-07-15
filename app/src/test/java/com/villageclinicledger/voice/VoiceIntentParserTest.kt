package com.villageclinicledger.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceIntentParserTest {

    @Test
    fun testRaviKitnaBakiHai() {
        val result = VoiceIntentParser.parse("Ravi kitna baki hai?")
        assertEquals(IntentType.SEARCH_BALANCE, result.intent)
        assertEquals("Ravi", result.patientName)
        assertNull(result.medicineAmount)
        assertNull(result.paymentAmount)
    }

    @Test
    fun testRamLalNePaanchSauDiye() {
        val result = VoiceIntentParser.parse("Ram Lal ne paanch sau diye")
        assertEquals(IntentType.PAYMENT, result.intent)
        assertEquals("Ram Lal", result.patientName)
        assertEquals(500.0, result.paymentAmount ?: 0.0, 0.01)
        assertNull(result.medicineAmount)
    }

    @Test
    fun testNayaPatientChotuJhilaiSeDedhSauDawa() {
        val result = VoiceIntentParser.parse("Naya patient Chotu Jhilai se dedh sau dawa")
        assertEquals(IntentType.NEW_PATIENT, result.intent)
        assertEquals("Chotu", result.patientName)
        assertEquals("Jhilai", result.villageName)
        assertEquals(150.0, result.medicineAmount ?: 0.0, 0.01)
        assertNull(result.paymentAmount)
    }

    @Test
    fun testRameshBairwaKoDawaDi() {
        val result = VoiceIntentParser.parse("Ramesh Bairwa ko dawa di")
        assertEquals(IntentType.MEDICINE, result.intent)
        assertEquals("Ramesh Bairwa", result.patientName)
    }

    @Test
    fun testGalatHoGayaHataDo() {
        val result = VoiceIntentParser.parse("galat ho gaya, hata do")
        assertEquals(IntentType.CORRECTION, result.intent)
    }

    @Test
    fun testMixedMedicineAndPaymentPhrase() {
        val result = VoiceIntentParser.parse("Ravi ko 300 ki dawa di aur 100 jama kiye")
        assertEquals(IntentType.MEDICINE_AND_PAYMENT, result.intent)
        assertEquals("Ravi", result.patientName)
        assertEquals(300.0, result.medicineAmount ?: 0.0, 0.01)
        assertEquals(100.0, result.paymentAmount ?: 0.0, 0.01)
    }

    @Test
    fun testExtractPatientName_withEnglishPhrases() {
        assertEquals("Ramesh", VoiceIntentParser.extractPatientName("Ramesh ko dawa di 200"))
        assertEquals("Ramesh", VoiceIntentParser.extractPatientName("dawa di Ramesh 200"))
        assertEquals("Suresh", VoiceIntentParser.extractPatientName("medicine for Suresh 500"))
    }

    @Test
    fun testExtractPatientName_withHindiPhrases() {
        assertEquals("रमेश", VoiceIntentParser.extractPatientName("रमेश को दवा दी २००"))
        assertEquals("रमेश", VoiceIntentParser.extractPatientName("दवा दी रमेश  २००"))
        assertEquals("सतीश", VoiceIntentParser.extractPatientName("बकाया बताओ सतीश का"))
    }

    @Test
    fun testExtractVillageName() {
        assertEquals("Siras", VoiceIntentParser.extractVillageName("Ramesh of Siras"))
        assertEquals("Jhilai", VoiceIntentParser.extractVillageName("ramesh of jhilai"))
    }

    @Test
    fun testMultiWordNamesMedicine() {
        // Multi-word name: "Ramesh Kumar Bairwa"
        val result = VoiceIntentParser.parse("Ramesh Kumar Bairwa ko 450 ki dawa di")
        assertEquals(IntentType.MEDICINE, result.intent)
        assertEquals("Ramesh Kumar Bairwa", result.patientName)
        assertEquals(450.0, result.medicineAmount ?: 0.0, 0.01)
        assertNull(result.paymentAmount)
    }

    @Test
    fun testMultiWordNamesPayment() {
        // Multi-word name: "Sita Devi Sharma"
        val result = VoiceIntentParser.parse("Sita Devi Sharma ne ek hazar rupees jama kiye")
        assertEquals(IntentType.PAYMENT, result.intent)
        assertEquals("Sita Devi Sharma", result.patientName)
        assertEquals(1000.0, result.paymentAmount ?: 0.0, 0.01)
        assertNull(result.medicineAmount)
    }

    @Test
    fun testMixedPaymentAndMedicineWithMultiWordName() {
        // Multi-word name: "Babu Lal Meena" with mixed payment and medicine sentence
        val result = VoiceIntentParser.parse("Babu Lal Meena ko teen sau ki dawa di aur unhone sau rupees jama kiye")
        assertEquals(IntentType.MEDICINE_AND_PAYMENT, result.intent)
        assertEquals("Babu Lal Meena", result.patientName)
        assertEquals(300.0, result.medicineAmount ?: 0.0, 0.01)
        assertEquals(100.0, result.paymentAmount ?: 0.0, 0.01)
    }

    @Test
    fun testNewPatientWithMultiWordNameAndVillage() {
        // Multi-word name: "Mahendra Singh Gurjar"
        val result = VoiceIntentParser.parse("Naya patient Mahendra Singh Gurjar Siras se paanch sau ki dawa di")
        assertEquals(IntentType.NEW_PATIENT, result.intent)
        assertEquals("Mahendra Singh Gurjar", result.patientName)
        assertEquals("Siras", result.villageName)
        assertEquals(500.0, result.medicineAmount ?: 0.0, 0.01)
        assertNull(result.paymentAmount)
    }
}
