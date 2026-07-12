package com.villageclinicledger.data.util

import android.content.Context
import android.util.Log
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.repository.PatientRepository
import java.util.Calendar
import java.util.Date
import java.util.Random

object DataSeeder {
    private const val TAG = "DataSeeder"

    suspend fun seedDatabaseIfNeeded(context: Context) {
        val database = VillageClinicLedgerDatabase.getDatabase(context)
        val patientDao = database.patientDao()
        val existingPatientsCount = patientDao.getAllPatientsSync().size

        if (existingPatientsCount >= 10) {
            Log.d(TAG, "Database already seeded with $existingPatientsCount patients. Skipping.")
            return
        }

        Log.d(TAG, "Seeding database with realistic demo data of 75 patients...")
        val repository = PatientRepository(context)
        val villages = database.villageDao().getAllVillagesSync()
        if (villages.isEmpty()) {
            Log.e(TAG, "No villages found in database. Cannot seed patients.")
            return
        }

        val random = Random(42) // Constant seed for reproducible data

        // 1. Create family groups across villages
        val familyNames = listOf(
            "Sharma Parivaar", "Meena Kunba", "Yadav Family", "Choudhary Gharana",
            "Jangid Parivaar", "Patel Clan", "Rajput Parivaar", "Gurjar Parivaar",
            "Saini Parivaar", "Mishra Parivaar", "Gupta Parivaar", "Joshi Family",
            "Tiwari Parivaar", "Kumawat Family", "Prajapat Clan"
        )
        val familyGroupIds = mutableListOf<Long>()
        for (i in familyNames.indices) {
            val village = villages[random.nextInt(villages.size)]
            val familyGroup = FamilyGroup(
                name = familyNames[i],
                villageId = village.id
            )
            val fgId = repository.insertFamilyGroup(familyGroup)
            familyGroupIds.add(fgId)
        }

        // 2. Realistic Indian names lists
        val maleFirstNames = listOf(
            "Aarav", "Rajesh", "Amit", "Sanjay", "Vijay", "Sunil", "Vikram", "Deepak",
            "Ramesh", "Suresh", "Manoj", "Anil", "Alok", "Mahesh", "Naresh", "Dinesh",
            "Rakesh", "Vinod", "Ashok", "Naresh", "Harish", "Satish", "Pramod", "Mukesh",
            "Santosh", "Subhash", "Arvind", "Ajay", "Rohit", "Vikas", "Pankaj", "Sandeep",
            "Gaurav", "Abhinav", "Pradeep", "Jitendra", "Dharmendra", "Surendra", "Mahendra",
            "Devendra", "Ravindra", "Gajendra"
        )

        val femaleFirstNames = listOf(
            "Sunita", "Anita", "Pinky", "Kavita", "Rekha", "Manju", "Seema", "Meena",
            "Pooja", "Aarti", "Jyoti", "Babita", "Suman", "Mamta", "Sarita", "Maya",
            "Sushila", "Lalita", "Sharda", "Kamla", "Vimla", "Geeta", "Sita", "Radha",
            "Rukmani", "Draupadi", "Vidya", "Urmila", "Asha", "Lata", "Pushpa"
        )

        val lastNames = listOf(
            "Sharma", "Verma", "Yadav", "Choudhary", "Meena", "Patel", "Kumar", "Singh",
            "Gupta", "Saini", "Mishra", "Jangid", "Kumawat", "Prajapat", "Gurjar", "Rajput",
            "Jain", "Rathore", "Joshi", "Tiwari"
        )

        val commonAliases = listOf(
            "Kalu", "Bittu", "Chhotu", "Munna", "Gullu", "Chiku", "Pappu", "Bablu", "Guddu", "Kaku",
            "Sinu", "Chunnu", "Lalu", "Pintu", "Raju", "Sonu", "Pinku", "Tinu"
        )

        val medNotes = listOf(
            "Paracetamol + Cough Syrup", "Amoxicillin course", "BP medicines (1 month)",
            "Calcium + Vitamin D", "Antacid + Painkillers", "Ointment for skin rash",
            "Diabetes checkup & Metformin", "Ear drops", "Eye drops & allergy tablets",
            "Multivitamins", "Fever checkup & IV fluids", "Bandage & antiseptic dressing"
        )

        // Seed 75 patients
        val patientsCount = 75
        for (i in 1..patientsCount) {
            val isMale = random.nextBoolean()
            val firstName = if (isMale) {
                maleFirstNames[random.nextInt(maleFirstNames.size)]
            } else {
                femaleFirstNames[random.nextInt(femaleFirstNames.size)]
            }
            val lastName = lastNames[random.nextInt(lastNames.size)]
            val patientName = "$firstName $lastName"

            // Choose village
            val village = villages[random.nextInt(villages.size)]

            // Filter family groups belonging to this village
            val villageFamilies = familyGroupIds.filter { fgId ->
                // Look up family group to verify village matching
                // For simplicity, we can map family index to the assigned village or just random assign.
                // Let's query family from database or just do a simple modulo mapping
                fgId % villages.size == village.id % villages.size
            }

            val familyGroupId = if (villageFamilies.isNotEmpty() && random.nextDouble() < 0.6) {
                villageFamilies[random.nextInt(villageFamilies.size)]
            } else {
                null
            }

            // Create realistic phone number
            val phonePrefix = listOf("98", "99", "97", "89", "88", "76", "77", "94")
            val phone = phonePrefix[random.nextInt(phonePrefix.size)] + String.format("%08d", random.nextInt(100000000))

            // Create patient
            val patient = Patient(
                name = patientName,
                villageId = village.id,
                phone = phone,
                familyGroupId = familyGroupId,
                createdAt = getRandomDateInPast(random, 90),
                updatedAt = Date()
            )

            val patientId = repository.insertPatient(patient)

            // Add Alias (~30% of patients)
            if (random.nextDouble() < 0.3) {
                val aliasText = commonAliases[random.nextInt(commonAliases.size)]
                repository.insertAlias(Alias(patientId = patientId, alias = aliasText))
            }

            // Create transactions
            val numTransactions = random.nextInt(6) // 0 to 5 transactions
            if (numTransactions > 0) {
                var runningBalance = 0.0
                for (t in 1..numTransactions) {
                    val daysAgo = random.nextInt(90)
                    val transactionDate = getRandomDateInPast(random, daysAgo)

                    // Decide transaction type
                    val randType = random.nextDouble()
                    val type = when {
                        randType < 0.6 -> "medicine"
                        randType < 0.9 -> "payment"
                        else -> "adjustment"
                    }

                    // Amounts are multiples of 10 or 50
                    val amount = when (type) {
                        "medicine" -> (random.nextInt(20) + 1) * 50.0 // 50 to 1000
                        "payment" -> {
                            // Pay some of running balance, or round amounts
                            if (runningBalance > 0) {
                                val payChoice = random.nextDouble()
                                when {
                                    payChoice < 0.4 -> runningBalance // Pay full
                                    payChoice < 0.8 -> (random.nextInt((runningBalance / 50).toInt().coerceAtLeast(1)) + 1) * 50.0 // Pay partial
                                    else -> 100.0
                                }
                            } else {
                                (random.nextInt(5) + 1) * 50.0
                            }
                        }
                        else -> (random.nextInt(10) + 1) * 10.0 // 10 to 100 adjustment
                    }

                    val notes = when (type) {
                        "medicine" -> medNotes[random.nextInt(medNotes.size)]
                        "payment" -> "Cash payment received"
                        "adjustment" -> if (random.nextBoolean()) "Waived off discount" else "Balance correction"
                        else -> ""
                    }

                    // Add transaction
                    val tx = Transaction(
                        patientId = patientId,
                        type = type,
                        amount = amount,
                        notes = notes,
                        createdAt = transactionDate,
                        updatedAt = transactionDate
                    )
                    repository.insertTransaction(tx)

                    // Update local running balance tracker
                    runningBalance += if (type == "medicine" || type == "adjustment") amount else -amount
                }
            }
        }
        Log.d(TAG, "Database seeded successfully with 75 patients!")
    }

    private fun getRandomDateInPast(random: Random, maxDaysAgo: Int): Date {
        val calendar = Calendar.getInstance()
        if (maxDaysAgo > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -random.nextInt(maxDaysAgo))
        }
        // Set random time of day
        calendar.set(Calendar.HOUR_OF_DAY, random.nextInt(24))
        calendar.set(Calendar.MINUTE, random.nextInt(60))
        calendar.set(Calendar.SECOND, random.nextInt(60))
        return calendar.time
    }
}
