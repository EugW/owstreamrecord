package pro.eugw.owstreamrecord

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
        Controllers.getMainController().buttonApplySettings.setOnMouseClicked {
            val ctrl = Controllers.getMainController()
            val cfg = ConfigController
            cfg.setProp("period", ctrl.textFieldPeriod.text)
            cfg.setProp("outputPath", ctrl.textFieldOutputFilePath.text)
            cfg.setProp("outputTemplate", ctrl.textAreaOutputTemplate.text)
            cfg.setProp("messages", ctrl.toggleButtonMessages.isSelected)
            cfg.saveConfig()
        }
        service.start()
    }

    override fun stop() {
        super.stop()
        service.running = false
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
    }

}