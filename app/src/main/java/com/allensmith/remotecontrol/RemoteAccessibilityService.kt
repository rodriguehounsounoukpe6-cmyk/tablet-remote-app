package com.allensmith.remotecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Service d'accessibilite qui fait tourner un petit serveur HTTP local (port 8080)
 * et traduit les requetes recues en actions sur la tablette :
 * navigation, retour, accueil, volume, etc.
 *
 * Aucune connexion Internet requise : tout se passe sur le reseau local (WiFi).
 */
class RemoteAccessibilityService : AccessibilityService() {

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private val PORT = 8080

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connecte, demarrage du serveur sur le port $PORT")
        startServer()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Rien a faire ici : ce service sert uniquement a executer des commandes,
        // pas a reagir aux evenements de l'ecran.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrompu")
    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket?.close()
        serverThread?.interrupt()
    }

    private fun startServer() {
        serverThread = Thread {
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "Serveur pret sur le port $PORT")
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur serveur: ${e.message}")
            }
        }
        serverThread?.start()
    }

    private fun handleClient(client: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val requestLine = reader.readLine() ?: return
            // Exemple: "GET /cmd?action=up HTTP/1.1"
            val parts = requestLine.split(" ")
            val path = if (parts.size >= 2) parts[1] else "/"

            val action = extractParam(path, "action")
            val ok = if (action != null) executeAction(action) else false

            val body = "{\"ok\":$ok,\"action\":\"${action ?: ""}\"}"
            val writer = PrintWriter(client.getOutputStream(), true)
            writer.print(
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n\r\n" +
                body
            )
            writer.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur client: ${e.message}")
        } finally {
            client.close()
        }
    }

    private fun extractParam(path: String, key: String): String? {
        val query = path.substringAfter("?", "")
        for (pair in query.split("&")) {
            val kv = pair.split("=")
            if (kv.size == 2 && kv[0] == key) return kv[1]
        }
        return null
    }

    private fun executeAction(action: String): Boolean {
        return when (action) {
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "recent" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "lock" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else false
            }
            "vol_up" -> adjustVolume(AudioManager.ADJUST_RAISE)
            "vol_down" -> adjustVolume(AudioManager.ADJUST_LOWER)
            "ok" -> tapCenter()
            "up" -> swipe(fromBottom = true)
            "down" -> swipe(fromBottom = false)
            "left" -> swipe(fromRight = true)
            "right" -> swipe(fromRight = false)
            else -> false
        }
    }

    private fun adjustVolume(direction: Int): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return true
    }

    private fun tapCenter(): Boolean {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val y = metrics.heightPixels / 2f
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Simule un glissement vertical ou horizontal, utilise pour approx.
     * la navigation "haut/bas/gauche/droite" sur un ecran tactile classique
     * (les tablettes n'ont pas de vraie navigation D-pad comme Android TV).
     */
    private fun swipe(fromBottom: Boolean? = null, fromRight: Boolean? = null): Boolean {
        val metrics = resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()
        val path = Path()

        when {
            fromBottom == true -> { path.moveTo(w / 2, h * 0.75f); path.lineTo(w / 2, h * 0.25f) }
            fromBottom == false -> { path.moveTo(w / 2, h * 0.25f); path.lineTo(w / 2, h * 0.75f) }
            fromRight == true -> { path.moveTo(w * 0.75f, h / 2); path.lineTo(w * 0.25f, h / 2) }
            fromRight == false -> { path.moveTo(w * 0.25f, h / 2); path.lineTo(w * 0.75f, h / 2) }
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, 200)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val TAG = "RemoteControlService"
    }
}
