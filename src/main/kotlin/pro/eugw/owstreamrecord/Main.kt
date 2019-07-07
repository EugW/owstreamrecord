package pro.eugw.owstreamrecord

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Main : Application() {

    private lateinit var service: VisionService
    override fun start(primaryStage: Stage) {
        service = VisionService()
        primaryStage.title = "OWStreamRecord"
        primaryStage.icons.add(Image(Main::class.java.getResourceAsStream("/icon.png")))
        primaryStage.scene = Scene(FXMLLoader.load<Parent?>(Main::class.java.getResource("/main.fxml")))
        primaryStage.isResizable = false
        primaryStage.show()
        analyzeConfig()
        val ctrl = Controllers.getMainController()
        ctrl.buttonApplySettings.setOnMouseClicked {
            val cfg = ConfigController
            cfg.setProp("period", ctrl.textFieldPeriod.text)
            cfg.setProp("outputPath", ctrl.textFieldOutputFilePath.text)
            cfg.setProp("outputTemplate", ctrl.textAreaOutputTemplate.text)
            cfg.setProp("messages", ctrl.toggleButtonMessages.isSelected)
            cfg.setProp("resetWLSR", ctrl.toggleButtonResetWLSR.isSelected)
            cfg.saveConfig()
        }
        service.start()
    }

    override fun stop() {
        super.stop()
        val kernel32 = Kernel32.INSTANCE
        kernel32.TerminateProcess(kernel32.GetCurrentProcess(), 0)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ConfigController()
            launch(Main::class.java)
        }
    }

    private fun analyzeConfig() {
        val cfg = ConfigController.getConfig()
        val ctrl = Controllers.getMainController()
        ctrl.textFieldPeriod.text = cfg["period"].asString
        ctrl.textFieldOutputFilePath.text = cfg["outputPath"].asString
        ctrl.textAreaOutputTemplate.text = cfg["outputTemplate"].asString
        ctrl.toggleButtonMessages.isSelected = cfg["messages"].asBoolean
        ctrl.toggleButtonResetWLSR.isSelected = cfg["resetWLSR"].asBoolean
    }

}