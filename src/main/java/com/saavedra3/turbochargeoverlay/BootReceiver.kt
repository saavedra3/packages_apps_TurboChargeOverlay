package com.saavedra3.turbochargeoverlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


//esto para abrir el servicio cuando arranque la ROM
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, TurboChargeService::class.java)
            // Esto es para asegurar que el sistema no lo mate al inicio
            context.startForegroundService(serviceIntent)
        }
    }
}