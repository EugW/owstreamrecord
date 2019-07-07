package pro.eugw.owstreamrecord

import com.google.gson.JsonObject
import javafx.scene.control.TextInputDialog
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

class WLTracker(wins: Int, losses: Int) {

    init {
        WLTracker.wins = wins
        WLTracker.losses = losses
        val ctrl = Controllers.getMainController()
        ctrl.labelWins.setOnMouseClicked {
            if (it.clickCount == 2) {
                val dlg = TextInputDialog()
                dlg.title = "Win input"
                val result = dlg.showAndWait()
                result.ifPresent { str ->
                    if (str.isNumber())
                        WLTracker.wins = str.toInt()
                }
            }
        }
        ctrl.labelLosses.setOnMouseClicked {
            if (it.clickCount == 2) {
                val dlg = TextInputDialog()
                dlg.title = "Loss input"
                val result = dlg.showAndWait()
                result.ifPresent { str ->
                    if (str.isNumber())
                        WLTracker.losses = str.toInt()
                }
            }
        }
    }

    companion object {
        private var wins = 0
        private var losses = 0
        fun updateSR(newSR: Int, originSR: Int): JsonObject {
            val jsonObject = JsonObject()
            if (newSR > originSR) {
                wins++
                if (ConfigController.getConfig()["messages"].asBoolean)
                    displayNotification("Win added")
            } else if (newSR < originSR) {
                losses++
                if (ConfigController.getConfig()["messages"].asBoolean)
                    displayNotification("Loss added")
            }
            jsonObject.addProperty("w", wins)
            jsonObject.addProperty("l", losses)
            return jsonObject
        }
        private fun displayNotification(text: String) {
            val tray = SystemTray.getSystemTray()
            val trayIcon = TrayIcon(ImageIO.read(Main::class.java.getResourceAsStream("/icon.png")))
            trayIcon.isImageAutoSize = true
            tray.add(trayIcon)
            trayIcon.displayMessage("OWStreamRecord", text, TrayIcon.MessageType.INFO)
        }
    }

}