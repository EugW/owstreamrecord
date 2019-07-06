package pro.eugw.owstreamrecord

import javafx.scene.control.*
import javafx.scene.image.ImageView

class MainController {

    lateinit var imageViewOWPreview: ImageView
    lateinit var textFieldPeriod: TextField
    lateinit var buttonApplySettings: Button
    lateinit var labelCurrentSR: Label
    lateinit var textFieldOutputFilePath: TextField
    lateinit var labelServiceStatus: Label
    lateinit var labelWins: Label
    lateinit var labelLosses: Label
    lateinit var textAreaOutputTemplate: TextArea
    lateinit var toggleButtonMessages: ToggleButton

    fun initialize() {
        Controllers.setMainController(this)
    }

}