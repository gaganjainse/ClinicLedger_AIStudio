package com.villageclinicledger.data.util

import android.content.Context
import android.util.Log
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.repository.PatientRepository
import androidx.room.withTransaction
import java.util.Calendar
import java.util.Date
import java.util.Random

object DataSeeder {
    private const val TAG = "DataSeeder"

    suspend fun seedDatabaseIfNeeded(context: Context) {
        val database = VillageClinicLedgerDatabase.getDatabase(context)
        val patientDao = database.patientDao()
        val existingPatientsCount = patientDao.getAllPatientsSync().size

        if (existingPatientsCount > 0) {
            Log.d(TAG, "Database already has patients. Skipping seeding to protect live data.")
            return
        }

        val repository = PatientRepository(context)

        // Seed initial villages if villages are empty
        val villageDao = database.villageDao()
        var villages = villageDao.getAllVillagesSync()
        if (villages.isEmpty()) {
            try {
                val db = database.openHelper.writableDatabase
                val seedVillages = listOf(
                    Pair("Siras", "सिरस"),
                    Pair("Mehtabpura", "मेहताबपुरा"),
                    Pair("Jhilai", "झिलाई"),
                    Pair("Bassi", "बस्सी"),
                    Pair("Shyosinghpura", "श्योसिंघपुरा"),
                    Pair("Mandaliya", "मंडालिया"),
                    Pair("Nala", "नला"),
                    Pair("Piplya", "पीपल्या")
                )
                for (v in seedVillages) {
                    db.execSQL("INSERT OR IGNORE INTO villages (name, name_hindi) VALUES ('${v.first}', '${v.second}')")
                }
                villages = villageDao.getAllVillagesSync()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding default villages: ${e.message}")
            }
        }

        Log.d(TAG, "Seeding database with improved, cohesive bilingual demo data...")
        if (villages.isEmpty()) {
            Log.e(TAG, "No villages found in database. Cannot seed patients.")
            return
        }

        val random = Random(42) // Consistent seed for reproducible data

        // Predefined, structured, multi-generational families
        val seedFamilies = listOf(
            SeedFamily(
                nameEng = "Sitaram Sharma's Family",
                nameHin = "सीताराम शर्मा का परिवार",
                headNameEng = "Sitaram Sharma",
                headNameHin = "सीताराम शर्मा",
                casteEng = "Sharma",
                casteHin = "शर्मा",
                villageName = "Siras",
                members = listOf(
                    SeedMember("Sitaram Sharma", "सीताराम शर्मा", "Grandfather", "दादाजी", true, "Sitaram"),
                    SeedMember("Saraswati Sharma", "सरस्वती शर्मा", "Grandmother", "दादीजी", false),
                    SeedMember("Ramesh Sharma", "रमेश शर्मा", "Father", "पिताजी", true, "Ramesh"),
                    SeedMember("Sunita Sharma", "सुनीता शर्मा", "Mother", "माताजी", false),
                    SeedMember("Sunil Sharma", "सुनील शर्मा", "Son", "पुत्र", true, "Sunil"),
                    SeedMember("Aarav Sharma", "आरव शर्मा", "Grandson", "पोता", true, "Aarav")
                )
            ),
            SeedFamily(
                nameEng = "Kailash Sharma's Family",
                nameHin = "कैलाश शर्मा का परिवार",
                headNameEng = "Kailash Sharma",
                headNameHin = "कैलाश शर्मा",
                casteEng = "Sharma",
                casteHin = "शर्मा",
                villageName = "Jhilai", // Same surname, completely separate family group in Jhilai
                members = listOf(
                    SeedMember("Kailash Sharma", "कैलाश शर्मा", "Grandfather", "दादाजी", true, "Kailash"),
                    SeedMember("Kamla Sharma", "कमला शर्मा", "Grandmother", "दादीजी", false),
                    SeedMember("Deepak Sharma", "दीपक शर्मा", "Father", "पिताजी", true, "Deepu"),
                    SeedMember("Rekha Sharma", "रेखा शर्मा", "Mother", "माताजी", false),
                    SeedMember("Kavita Sharma", "कविता शर्मा", "Daughter", "पुत्री", false)
                )
            ),
            SeedFamily(
                nameEng = "Ramprasad Meena's Family",
                nameHin = "रामप्रसाद मीना का परिवार",
                headNameEng = "Ramprasad Meena",
                headNameHin = "रामप्रसाद मीना",
                casteEng = "Meena",
                casteHin = "मीना",
                villageName = "Mehtabpura",
                members = listOf(
                    SeedMember("Ramprasad Meena", "रामप्रसाद मीना", "Grandfather", "दादाजी", true),
                    SeedMember("Dhanni Meena", "धन्नी मीना", "Grandmother", "दादीजी", false),
                    SeedMember("Kalu Meena", "कालू मीना", "Father", "पिताजी", true, "Kalu"),
                    SeedMember("Meera Meena", "मीरा मीना", "Mother", "माताजी", false),
                    SeedMember("Chotu Meena", "छोटू मीना", "Son", "पुत्र", true, "Chhotu")
                )
            ),
            SeedFamily(
                nameEng = "Kalyan Meena's Family",
                nameHin = "कल्याण मीना का परिवार",
                headNameEng = "Kalyan Meena",
                headNameHin = "कल्याण मीना",
                casteEng = "Meena",
                casteHin = "मीना",
                villageName = "Bassi", // Another separate Meena family in Bassi
                members = listOf(
                    SeedMember("Kalyan Meena", "कल्याण मीना", "Grandfather", "दादाजी", true),
                    SeedMember("Suman Meena", "सुमन मीना", "Mother", "माताजी", false),
                    SeedMember("Pappu Meena", "पप्पू मीना", "Son", "पुत्र", true, "Pappu")
                )
            ),
            SeedFamily(
                nameEng = "Hajari Lal Yadav's Family",
                nameHin = "हजारी लाल यादव का परिवार",
                headNameEng = "Hajari Lal Yadav",
                headNameHin = "हजारी लाल यादव",
                casteEng = "Yadav",
                casteHin = "यादव",
                villageName = "Siras",
                members = listOf(
                    SeedMember("Hajari Lal Yadav", "हजारी लाल यादव", "Grandfather", "दादाजी", true),
                    SeedMember("Meera Devi Yadav", "मीरा देवी यादव", "Grandmother", "दादीजी", false),
                    SeedMember("Sanjay Yadav", "संजय यादव", "Father", "पिताजी", true, "Sanjay"),
                    SeedMember("Rekha Yadav", "रेखा यादव", "Mother", "माताजी", false),
                    SeedMember("Vijay Yadav", "विजय यादव", "Son", "पुत्र", true, "Vijay")
                )
            ),
            SeedFamily(
                nameEng = "Devi Lal Choudhary's Family",
                nameHin = "देवी लाल चौधरी का परिवार",
                headNameEng = "Devi Lal Choudhary",
                headNameHin = "देवी लाल चौधरी",
                casteEng = "Choudhary",
                casteHin = "चौधरी",
                villageName = "Siras",
                members = listOf(
                    SeedMember("Devi Lal Choudhary", "देवी लाल चौधरी", "Grandfather", "दादाजी", true),
                    SeedMember("Sharda Choudhary", "शारदा चौधरी", "Grandmother", "दादीजी", false),
                    SeedMember("Rajesh Choudhary", "राजेश चौधरी", "Father", "पिताजी", true, "Rajesh"),
                    SeedMember("Anita Choudhary", "अनीता चौधरी", "Mother", "माताजी", false),
                    SeedMember("Seema Choudhary", "सीमा चौधरी", "Daughter", "पुत्री", false)
                )
            ),
            SeedFamily(
                nameEng = "Bhairon Singh Rajput's Family",
                nameHin = "भैरों सिंह राजपूत का परिवार",
                headNameEng = "Bhairon Singh Rajput",
                headNameHin = "भैरों सिंह राजपूत",
                casteEng = "Rajput",
                casteHin = "राजपूत",
                villageName = "Mandaliya",
                members = listOf(
                    SeedMember("Bhairon Singh Rajput", "भैरों सिंह राजपूत", "Grandfather", "दादाजी", true, "Bhairon"),
                    SeedMember("Rukmani Rajput", "रुक्मणी राजपूत", "Grandmother", "दादीजी", false),
                    SeedMember("Vikram Singh Rajput", "विक्रम सिंह राजपूत", "Father", "पिताजी", true, "Vikram"),
                    SeedMember("Pushpa Rajput", "पुष्पा राजपूत", "Mother", "माताजी", false),
                    SeedMember("Amit Singh Rajput", "अमित सिंह राजपूत", "Son", "पुत्र", true, "Amit")
                )
            ),
            SeedFamily(
                nameEng = "Prabhu Lal Saini's Family",
                nameHin = "प्रभु लाल सैनी का परिवार",
                headNameEng = "Prabhu Lal Saini",
                headNameHin = "प्रभु लाल सैनी",
                casteEng = "Saini",
                casteHin = "सैनी",
                villageName = "Piplya",
                members = listOf(
                    SeedMember("Prabhu Lal Saini", "प्रभु लाल सैनी", "Grandfather", "दादाजी", true),
                    SeedMember("Babita Saini", "बबीता सैनी", "Mother", "माताजी", false),
                    SeedMember("Vijay Saini", "विजय सैनी", "Son", "पुत्र", true, "Vijay")
                )
            )
        )

        val medNotes = listOf(
            "Paracetamol + Cough Syrup", "Amoxicillin course", "BP medicines (1 month)",
            "Calcium + Vitamin D", "Antacid + Painkillers", "Ointment for skin rash",
            "Diabetes checkup & Metformin", "Ear drops", "Eye drops & allergy tablets",
            "Multivitamins", "Fever checkup & IV fluids", "Bandage & antiseptic dressing"
        )

        // Seed structured families first
        for (fam in seedFamilies) {
            val village = villages.firstOrNull { it.name.equals(fam.villageName, ignoreCase = true) } ?: villages[0]
            val familyGroup = FamilyGroup(
                name = "${fam.nameEng} / ${fam.nameHin}",
                caste = "${fam.casteEng} / ${fam.casteHin}",
                familyHeadName = "${fam.headNameEng} / ${fam.headNameHin}",
                villageId = village.id
            )
            val fgId = repository.insertFamilyGroup(familyGroup)

            var headPatientId: Long? = null

            for (m in fam.members) {
                val phonePrefix = listOf("98", "99", "97", "89", "88", "76", "77", "94")
                val phone = phonePrefix[random.nextInt(phonePrefix.size)] + String.format("%08d", random.nextInt(100000000))
                val patientName = "${m.nameEng} / ${m.nameHin}"
                val relationship = "${m.relationshipEng} / ${m.relationshipHin}"

                val pastDate = getRandomDateInPast(random, 90)
                val patient = Patient(
                    name = patientName,
                    villageId = village.id,
                    phone = phone,
                    familyGroupId = fgId,
                    relationship = relationship,
                    createdAt = pastDate,
                    updatedAt = pastDate
                )
                val pId = database.patientDao().insertPatient(patient)

                if (m.nameEng == fam.headNameEng) {
                    headPatientId = pId
                }

                // Add Alias if defined
                if (!m.alias.isNullOrBlank()) {
                    repository.insertAlias(Alias(patientId = pId, alias = m.alias))
                }

                // Create random transaction history for this patient
                createRandomTransactions(pId, repository, random, medNotes)
            }

            // Update family group with its head patient ID
            if (headPatientId != null) {
                val updatedFG = familyGroup.copy(id = fgId, headPatientId = headPatientId)
                database.familyGroupDao().updateFamilyGroup(updatedFG)
            }
        }

        // Seed 25 independent/individual patients to round out the ledger
        val independentNames = listOf(
            BilingualPair("Suresh Verma", "सुरेश वर्मा"),
            BilingualPair("Anita Sharma", "अनीता शर्मा"),
            BilingualPair("Kalu Ram Patel", "कालू राम पटेल"),
            BilingualPair("Dinesh Kumawat", "दिनेश कुमावत"),
            BilingualPair("Manoj Saini", "मनोज सैनी"),
            BilingualPair("Sunita Yadav", "सुनीता यादव"),
            BilingualPair("Vikram Singh", "विक्रम सिंह"),
            BilingualPair("Pooja Gurjar", "पूजा गुर्जर"),
            BilingualPair("Harish Tiwari", "हरीश तिवारी"),
            BilingualPair("Suman Jain", "सुमन जैन"),
            BilingualPair("Rajesh Meena", "राजेश मीना"),
            BilingualPair("Seema Prajapat", "सीमा प्रजापत"),
            BilingualPair("Subhash Gupta", "सुभाष गुप्ता"),
            BilingualPair("Gopal Verma", "गोपाल वर्मा"),
            BilingualPair("Rekha Choudhary", "रेखा चौधरी"),
            BilingualPair("Anil Joshi", "अनिल जोशी"),
            BilingualPair("Babita Rathore", "बबीता राठौड़"),
            BilingualPair("Sunil Kumar", "सुनील कुमार"),
            BilingualPair("Mamta Saini", "ममता सैनी"),
            BilingualPair("Aarav Mishra", "आरव मिश्रा"),
            BilingualPair("Maya Patel", "माया पटेल"),
            BilingualPair("Gajendra Rajput", "गजेन्द्र राजपूत"),
            BilingualPair("Sharda Devi", "शारदा देवी"),
            BilingualPair("Vinod Jangid", "विनोद जांगिड़"),
            BilingualPair("Pushpa Sharma", "पुष्पा शर्मा")
        )

        for (namePair in independentNames) {
            val village = villages[random.nextInt(villages.size)]
            val phonePrefix = listOf("98", "99", "97", "89", "88", "76", "77", "94")
            val phone = phonePrefix[random.nextInt(phonePrefix.size)] + String.format("%08d", random.nextInt(100000000))
            
            val pastDate = getRandomDateInPast(random, 90)
            val patient = Patient(
                name = "${namePair.eng} / ${namePair.hin}",
                villageId = village.id,
                phone = phone,
                familyGroupId = null,
                relationship = "",
                createdAt = pastDate,
                updatedAt = pastDate
            )
            val pId = database.patientDao().insertPatient(patient)

            // Random Alias (~20%)
            if (random.nextDouble() < 0.2) {
                val aliases = listOf("Kaku", "Sinu", "Chunnu", "Lalu", "Pintu", "Raju", "Sonu", "Pinku")
                repository.insertAlias(Alias(patientId = pId, alias = aliases[random.nextInt(aliases.size)]))
            }

            // Create random transactions
            createRandomTransactions(pId, repository, random, medNotes)
        }

        Log.d(TAG, "Database seeded successfully with 8 family groups representing actual households and 25 independent patients!")
    }

    private suspend fun createRandomTransactions(
        patientId: Long,
        repository: PatientRepository,
        random: Random,
        medNotes: List<String>
    ) {
        val numTransactions = random.nextInt(5) // 0 to 4 transactions
        if (numTransactions > 0) {
            var runningBalance = 0.0
            for (t in 1..numTransactions) {
                val daysAgo = random.nextInt(90)
                val transactionDate = getRandomDateInPast(random, daysAgo)

                val randType = random.nextDouble()
                val type = when {
                    randType < 0.6 -> "medicine"
                    randType < 0.9 -> "payment"
                    else -> "adjustment"
                }

                val amount = when (type) {
                    "medicine" -> (random.nextInt(15) + 1) * 50.0 // 50 to 750
                    "payment" -> {
                        if (runningBalance > 0) {
                            val payChoice = random.nextDouble()
                            when {
                                payChoice < 0.5 -> runningBalance // Pay full
                                payChoice < 0.8 -> (random.nextInt((runningBalance / 50).toInt().coerceAtLeast(1)) + 1) * 50.0
                                else -> 100.0
                            }
                        } else {
                            (random.nextInt(4) + 1) * 50.0
                        }
                    }
                    else -> (random.nextInt(5) + 1) * 10.0 // 10 to 50 adjustment
                }

                val notes = when (type) {
                    "medicine" -> medNotes[random.nextInt(medNotes.size)]
                    "payment" -> "Cash payment received"
                    "adjustment" -> if (random.nextBoolean()) "Waived off discount" else "Balance correction"
                    else -> ""
                }

                val tx = Transaction(
                    patientId = patientId,
                    type = type,
                    amount = amount,
                    notes = notes,
                    createdAt = transactionDate,
                    updatedAt = transactionDate
                )
                repository.insertTransaction(tx)

                runningBalance += if (type == "medicine" || type == "adjustment") amount else -amount
            }
        }
    }

    private fun getRandomDateInPast(random: Random, maxDaysAgo: Int): Date {
        val calendar = Calendar.getInstance()
        if (maxDaysAgo > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -random.nextInt(maxDaysAgo))
        }
        calendar.set(Calendar.HOUR_OF_DAY, random.nextInt(24))
        calendar.set(Calendar.MINUTE, random.nextInt(60))
        calendar.set(Calendar.SECOND, random.nextInt(60))
        return calendar.time
    }

    private data class SeedFamily(
        val nameEng: String,
        val nameHin: String,
        val headNameEng: String,
        val headNameHin: String,
        val casteEng: String,
        val casteHin: String,
        val villageName: String,
        val members: List<SeedMember>
    )

    private data class SeedMember(
        val nameEng: String,
        val nameHin: String,
        val relationshipEng: String,
        val relationshipHin: String,
        val isMale: Boolean,
        val alias: String? = null
    )

    private data class BilingualPair(
        val eng: String,
        val hin: String
    )

    suspend fun clearAllData(context: Context) {
        val database = VillageClinicLedgerDatabase.getDatabase(context)
        database.withTransaction {
            database.transactionDao().deleteAll()
            database.aliasDao().deleteAll()
            database.patientDao().deleteAll()
            database.familyGroupDao().deleteAll()
            database.villageDao().deleteAll()
        }
        // Re-seed empty default villages
        val villageDao = database.villageDao()
        val seedVillages = listOf(
            Pair("Siras", "सिरस"),
            Pair("Mehtabpura", "मेहताबपुरा"),
            Pair("Jhilai", "झिलाई"),
            Pair("Bassi", "बस्सी"),
            Pair("Shyosinghpura", "श्योसिंघपुरा"),
            Pair("Mandaliya", "मंडालिया"),
            Pair("Nala", "नला"),
            Pair("Piplya", "पीपल्या")
        )
        try {
            val db = database.openHelper.writableDatabase
            for (v in seedVillages) {
                db.execSQL("INSERT OR IGNORE INTO villages (name, name_hindi) VALUES ('${v.first}', '${v.second}')")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error re-seeding default villages: ${e.message}")
        }
    }

    suspend fun seedDemoDataForce(context: Context) {
        clearAllData(context)
        seedDatabaseIfNeeded(context)
    }
}
