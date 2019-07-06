package pro.eugw.owstreamrecord

import com.sun.jna.Memory
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinGDI
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.paint.Color
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.io.File

class VisionService : Thread() {

    var running = true
    private var firstRun = true

    override fun run() {
        super.run()
        val dataFile = File("eng.traineddata")
        if (!dataFile.exists()) {
            dataFile.writeBytes(Main::class.java.getResource("/eng.traineddata").readBytes())
        }
        val tess = Tesseract()
        Platform.runLater {
            Controllers.getMainController().labelServiceStatus.textFill = Color(0.0, 1.0, 0.0, 1.0)
            Controllers.getMainController().labelServiceStatus.text = "Active"
        }
        while (running) {
            val window = User32.INSTANCE.FindWindow(null, "overwatch")
            if (window != null) {
                val img = capture(window)
                Platform.runLater { Controllers.getMainController().imageViewOWPreview.image = SwingFXUtils.toFXImage(img, null) }
                if (img != null) {
                    var modeLarge = true
                    if (img.getRGB(970, 549) != img.getRGB(970, 498))
                        modeLarge = false
                    val subImg = if (modeLarge) img.getSubimage(1100, 500, 100, 45) else img.getSubimage(1075, 512, 97, 37)
                    val h = subImg.height
                    val w = subImg.width
                    if (subImg.getRGB(0,0) == subImg.getRGB(w - 1, h - 1)) {
                        val ocr = tess.doOCR(subImg).removeSuffix("\n")
                        val testArr = ocr.toCharArray()
                        var digital = true
                        testArr.forEach {
                            if (!it.isDigit())
                                digital = false
                        }
                        if (digital && ocr.isNotBlank()) {
                            val sr = ocr.toInt()
                            if (sr in 0..5000) {
                                if (firstRun) {
                                    WLTracker.initialize(0, 0, sr)
                                    firstRun = false
                                } else {
                                    val obj = WLTracker.updateSR(sr)
                                    Platform.runLater {
                                        Controllers.getMainController().labelCurrentSR.text = sr.toString()
                                        Controllers.getMainController().labelWins.text = obj["w"].asString
                                        Controllers.getMainController().labelLosses.text = obj["l"].asString
                                    }
                                    File(ConfigController.getConfig()["outputPath"].asString).writeText(ConfigController.getConfig()["outputTemplate"].asString.replace("%w", obj["w"].asString).replace("%l", obj["l"].asString).replace("%sr", sr.toString()))
                                }
                            }
                        }
                    }
                }
            }
            sleep(ConfigController.getConfig().get("period").asLong)
        }
        Platform.runLater {
            Controllers.getMainController().labelServiceStatus.textFill = Color(1.0, 0.0, 0.0, 1.0)
            Controllers.getMainController().labelServiceStatus.text = "Inactive"
        }
    }

    private fun capture(hWnd: WinDef.HWND): BufferedImage? {
        val hdcWindow = User32.INSTANCE.GetDC(hWnd)
        val hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow)
        val bounds = WinDef.RECT()
        User32.INSTANCE.GetClientRect(hWnd, bounds)
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (height == 0 || width == 0)
            return null
        val hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height)
        val hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap)
        GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, GDI32.SRCCOPY)
        GDI32.INSTANCE.SelectObject(hdcMemDC, hOld)
        GDI32.INSTANCE.DeleteDC(hdcMemDC)
        val bmi = WinGDI.BITMAPINFO()
        bmi.bmiHeader.biWidth = width
        bmi.bmiHeader.biHeight = -height
        bmi.bmiHeader.biPlanes = 1
        bmi.bmiHeader.biBitCount = 32
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB
        val buffer = Memory((width * height * 4).toLong())
        GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width)
        GDI32.INSTANCE.DeleteObject(hBitmap)
        User32.INSTANCE.ReleaseDC(hWnd, hdcWindow)
        return image
    }
}