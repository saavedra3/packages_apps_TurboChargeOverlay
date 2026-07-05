package com.saavedra3.turbochargeoverlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class TurboChargeService : Service() {

    private val DEVELOPER = "@saavedra3"
    private val TAG = "TurboChargeOverlay"
    private var overlayView: View? = null


    //para el bucle de partículas
    private val handlerParticulas = Handler(Looper.getMainLooper())
    private var runnableParticulas: Runnable? = null




    //el recibidor para leer la bateria
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                // ver si está conectado a la corriente
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                // ver qué tipo de cargador es
                val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                val wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

                Log.d(TAG, "Estado de carga: ${if (isCharging) "CONECTADO" else "DESCONECTADO"}")
                Log.d(TAG, "Tipo: ${if (acCharge) "AC/Pared" else if (usbCharge) "USB" else if (wirelessCharge) "Inalámbrico" else "Desconocido"}")

                if (isCharging) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = (level * 100 / scale.toFloat()).toInt()

                    calcularPotenciaYMostrar(context, intent, batteryPct)
                } else {
                    Log.d(TAG, "No está cargando, omitiendo cálculo.")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "¡El servicio inicio")
        Log.d(TAG, "TurboChargeOverlay v1.0 | Dev: $DEVELOPER")

        // detector de batería para que siempre esté escuchando
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun calcularPotenciaYMostrar(context: Context, batteryIntent: Intent, batteryPct: Int) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAmps = Math.abs(currentMicroAmps) / 1000000f

        val voltageMillivolts = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val voltageVolts = voltageMillivolts / 1000f

        val watts = voltageVolts * currentAmps

        Log.d(TAG, "Cargando: ${String.format("%.2f", watts)}W | Nivel: $batteryPct%")
        // Para probar en USB mantengo el 0.5f pero son como 10
        if (watts >= 10.0f) {
            mostrarOverlay(watts, batteryPct)
        }
    }


    @Suppress("DEPRECATION")
    private fun mostrarOverlay(watts: Float, bateriaPct: Int) {
        if (overlayView != null) return

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_carga, null)

        // actualizar textos
        val textoBateria = overlayView?.findViewById<TextView>(R.id.texto_porcentaje)
        val textoWatts = overlayView?.findViewById<TextView>(R.id.texto_watts)
        textoBateria?.text = "$bateriaPct%"

        // Redondeamos los vatios
        val wattsRedondeados = String.format("%.1f", watts)
        textoWatts?.text = "${wattsRedondeados}W"

        // Animar el Resplandor (Efecto Latido)
        val resplandor = overlayView?.findViewById<ImageView>(R.id.resplandor_central)
        val latidoX = ObjectAnimator.ofFloat(resplandor, "scaleX", 1.0f, 1.2f)
        val latidoY = ObjectAnimator.ofFloat(resplandor, "scaleY", 1.0f, 1.2f)
        latidoX.repeatCount = ValueAnimator.INFINITE
        latidoX.repeatMode = ValueAnimator.REVERSE
        latidoY.repeatCount = ValueAnimator.INFINITE
        latidoY.repeatMode = ValueAnimator.REVERSE
        latidoX.duration = 1500 // 1 y medio segundo
        latidoY.duration = 1500
        latidoX.start()
        latidoY.start()

        // arrancar las particulas
        val contenedorParticulas = overlayView?.findViewById<FrameLayout>(R.id.contenedor_particulas)
        if (contenedorParticulas != null) {
            iniciarEfectoParticulas(contenedorParticulas)
        }

        //ventana
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_SHOW_WHEN_LOCKED y FLAG_TURN_SCREEN_ON para ver si se muestra sobre la pantalla de bloqueo (quiza)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(overlayView, params)

        overlayView?.setOnClickListener { quitarOverlay(windowManager) }

        //lo que tarda en desaparecer la pantalla
        Handler(Looper.getMainLooper()).postDelayed({
            quitarOverlay(windowManager)
        }, 8000)
    }

    private fun iniciarEfectoParticulas(contenedor: FrameLayout) {
        runnableParticulas = object : Runnable {
            override fun run() {
                if (contenedor.width > 0 && contenedor.height > 0) {
                    crearParticula(contenedor)
                }
                handlerParticulas.postDelayed(this, 100)
            }
        }
        handlerParticulas.post(runnableParticulas!!)
    }



    // una bestia esta funcion que sigue
    private fun crearParticula(contenedor: FrameLayout) {
        val particula = ImageView(this)
        particula.setImageResource(R.drawable.bg_particula)

        val tamaño = (20..40).random()
        val params = FrameLayout.LayoutParams(tamaño, tamaño)

        //la IA ayudo un monton aqui
        // 1. PUNTO DE PARTIDA: Exactamente en el centro inferior
        // Dividimos el ancho a la mitad y le restamos la mitad del tamaño de la partícula para centrarla perfecto
        val startX = (contenedor.width / 2f) - (tamaño / 2f)
        val startY = contenedor.height.toFloat() // Abajo del todo

        particula.x = startX
        particula.y = startY
        contenedor.addView(particula, params)

        // 2. PUNTO DE DESTINO (Dispersión)
        // Generamos un número aleatorio entre -400 (izquierda) y +400 (derecha)
        val desviacionHorizontal = (-400..400).random().toFloat()
        val endX = startX + desviacionHorizontal

        // Hacemos que suban una distancia aleatoria (entre 800 y 1200 píxeles)
        val endY = startY - (800..1200).random().toFloat()

        val duracion = (1500..3000).random().toLong()

        // 3. ANIMACIONES (Ahora animamos también el eje X)
        val moverX = ObjectAnimator.ofFloat(particula, "translationX", startX, endX)
        val moverY = ObjectAnimator.ofFloat(particula, "translationY", startY, endY)
        val desvanecer = ObjectAnimator.ofFloat(particula, "alpha", 1f, 0f)

        val animatorSet = AnimatorSet()
        // Ejecutamos las tres cosas a la vez: subir, irse de lado, y hacerse transparente
        animatorSet.playTogether(moverX, moverY, desvanecer)
        animatorSet.duration = duracion
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // ¡Limpiamos la memoria!
                contenedor.removeView(particula)
            }
        })
        animatorSet.start()
    }

    private fun quitarOverlay(windowManager: WindowManager) {
        if (overlayView != null) {
            // Detenemos el generador de partículas
            runnableParticulas?.let { handlerParticulas.removeCallbacks(it) }
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        quitarOverlay(windowManager)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}