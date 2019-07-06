package pro.eugw.owstreamrecord

import com.google.gson.JsonObject
import javafx.scene.control.TextInputDialog
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

class WLTracker {

    companion object {
        private var wins = 0
        private var losses = 0
        private var originSR = 0
        fun updateSR(newSR: Int): JsonObject {
            val jsonObject = JsonObject()
            if (newSR > originSR) {
                wins++
                if (ConfigController.getConfig()["messages"].asBoolean)
                    displayNotification("Win added")
            }
            if (newSR < originSR) {
                losses++
                if (ConfigController.getConfig()["messages"].asBoolean)
                    displayNotification("Loss added")
            }
            originSR = newSR
            jsonObject.addProperty("w", wins)
            jsonObject.addProperty("l", losses)
            return jsonObject
        }
        fun initialize(wins: Int, losses: Int, originSR: Int) {
            WLTracker.wins = wins
            WLTracker.losses = losses
            WLTracker.originSR = originSR
            val ctrl = Controllers.getMainController()
            ctrl.labelWins.setOnMouseClicked {
                if (it.clickCount == 2) {
                    val dlg = TextInputDialog()
                    dlg.title = "Win input"
                    val result = dlg.showAndWait()
                    result.ifPresent { str ->
                        var digital = true
                        str.toCharArray().forEach { ch ->
                            if (!ch.isDigit())
                                digital = false
                        }
                        if (digital)
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
                        var digital = true
                        str.toCharArray().forEach { ch ->
                            if (!ch.isDigit())
                                digital = false
                        }
                        if (digital)
                            WLTracker.losses = str.toInt()
                    }
                }
            }
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