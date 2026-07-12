package com.villageclinicledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.converters.DateConverter

/**
 * The Room database for the Village Clinic Ledger application.
 * Contains four entities: Patient, Village, Alias, and Transaction.
 * Uses [DateConverter] to persist java.util.Date as Long timestamps.
 *
 * The singleton pattern (via [getDatabase]) ensures only one database
 * instance exists per application process. Foreign keys are enabled
 * on open via PRAGMA to enforce referential integrity.
 */
@Database(
    entities = [
        Patient::class,
        Village::class,
        Alias::class,
        Transaction::class,
        FamilyGroup::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class VillageClinicLedgerDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao

    abstract fun villageDao(): VillageDao

    abstract fun aliasDao(): AliasDao

    abstract fun transactionDao(): TransactionDao

    abstract fun familyGroupDao(): FamilyGroupDao

    companion object {
        @Volatile
        private var INSTANCE: VillageClinicLedgerDatabase? = null

        private val SEED_VILLAGES = listOf(
            "Siras", "Mehtabpura", "Jhilai", "Bassi",
            "Shyosinghpura", "Mandaliya", "Nala", "Piplya"
        )

        fun getDatabase(context: Context): VillageClinicLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VillageClinicLedgerDatabase::class.java,
                    "village_clinic_ledger.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            for (village in SEED_VILLAGES) {
                                db.execSQL("INSERT OR IGNORE INTO villages (name) VALUES ('$village')")
                            }
                        }

                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
