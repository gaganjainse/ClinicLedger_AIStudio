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
    version = 4,
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
            Pair("Siras", "सिरस"),
            Pair("Mehtabpura", "मेहताबपुरा"),
            Pair("Jhilai", "झिलाई"),
            Pair("Bassi", "बस्सी"),
            Pair("Shyosinghpura", "श्योसिंघपुरा"),
            Pair("Mandaliya", "मंडालिया"),
            Pair("Nala", "नला"),
            Pair("Piplya", "पीपल्या")
        )

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `family_groups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `head_patient_id` INTEGER, 
                        `village_id` INTEGER NOT NULL, 
                        `created_at` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_family_groups_village_id` ON `family_groups` (`village_id`)")
                db.execSQL("ALTER TABLE `patients` ADD COLUMN `family_group_id` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `villages` ADD COLUMN `name_hindi` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `patients` ADD COLUMN `relationship` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_groups` ADD COLUMN `caste` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `family_groups` ADD COLUMN `family_head_name` TEXT NOT NULL DEFAULT ''")
            }
        }

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
                                db.execSQL("INSERT OR IGNORE INTO villages (name, name_hindi) VALUES ('${village.first}', '${village.second}')")
                            }
                        }

                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
