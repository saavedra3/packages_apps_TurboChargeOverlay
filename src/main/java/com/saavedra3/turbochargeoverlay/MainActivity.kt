package com.saavedra3.turbochargeoverlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
//ME GUSTAN LAS NOTAS CUANDO PROGRAMO PERDON...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, TurboChargeService::class.java)
        startService(intent) // Si el servicio ya está corriendo, esto no lo duplica.
        // No necesito UI ni nada relevante para graficos...

        //esto es para testing solamente
        solicitarPermisoOverlay()
        finish()
    }


    //Funcion para solicitar los permisos internos de android
    //Esto es mas que todo para testing porque la app va en PRIV-APP
    private fun solicitarPermisoOverlay() {
        // Verifica primero
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Sin permiso para mostrar sobre otras apps", Toast.LENGTH_LONG).show()

            // un intento que abre la ventana de ajustes para slicitar el permiso
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            //Toast.makeText(this, "Hay permiso", Toast.LENGTH_SHORT).show()
            // inicio el servicio
            val serviceIntent = Intent(this, TurboChargeService::class.java)
            startService(serviceIntent)

            // Cerramos la actividad principal para que no estorbe (la app queda en segundo plano)
            finish()
        }
    }
}