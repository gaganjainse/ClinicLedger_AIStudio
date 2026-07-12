package com.villageclinicledger

import android.app.Application
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase

class VillageClinicLedgerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VillageClinicLedgerApp
            private set

        fun getDatabase(): VillageClinicLedgerDatabase {
            return VillageClinicLedgerDatabase.getDatabase(instance)
        }
    }
}
