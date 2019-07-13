package pro.eugw.owstreamrecord

class Controllers {

    companion object {
        private var mainController: MainController? = null
        fun getMainController() = mainController!!
        fun setMainController(mainController: MainController) {
            Controllers.mainController = mainController
        }
    }

}