/**
 * Service for handling voice-based patient lookup in the background.
 *
 * This service is a stub/placeholder — it is declared in the manifest to reserve
 * the `FOREGROUND_SERVICE` permission and will eventually listen for voice input
 * (e.g. via the `RECORD_AUDIO` permission) to query patient records hands-free,
 * which is critical for clinic staff who may be occupied with patients.
 *
 * Architectural decisions:
 * - Extends [android.app.Service] rather than a bound-service pattern so it can
 *   run independently of any one Activity's lifecycle.
 * - Returns [START_NOT_STICKY] to avoid unnecessary restarts; the voice session
 *   is expected to be started explicitly by the user from the UI.
 */
package com.villageclinicledger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class VoiceService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
