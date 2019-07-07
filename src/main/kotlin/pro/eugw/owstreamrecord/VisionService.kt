package pro.eugw.owstreamrecord

import com.sun.jna.Memory
import com.sun.jna.platform.win32.*
import com.sun.jna.ptr.IntByReference
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
        var prevSR = 0
        val user32 = User32.INSTANCE
        val kernel32 = Kernel32.INSTANCE
        val psapi = Psapi.INSTANCE
        while (running) {
            var window: WinDef.HWND? = null
            user32.EnumWindows({ hWnd, _ ->
                val charArr = CharArray(512)
                user32.GetWindowText(hWnd, charArr, 512)
                val winName = String(charArr.dropLastWhile { !it.isLetterOrDigit() }.toCharArray())
                val charArr2 = CharArray(512)
                val proc = IntByReference()
                user32.GetWindowThreadProcessId(hWnd, proc)
                psapi.GetModuleFileNameExW(kernel32.OpenProcess(Kernel32.PROCESS_QUERY_LIMITED_INFORMATION, false, proc.value), null, charArr2, 512)
                val path = String(charArr2.dropLastWhile { !it.isLetterOrDigit() }.toCharArray()).split("\\").last()
                if (winName == "Overwatch" && path == "Overwatch.exe")
                    window = hWnd
                    true
            },null)
            if (window != null) {
                val img = capture(window as WinDef.HWND)
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
                        if (ocr.isNumber() && ocr.isNotBlank()) {
                            val sr = ocr.toInt()
                            if (sr in 0..5000) {
                                if (firstRun) {
                                    prevSR = sr
                                    WLTracker(0, 0)
                                    Platform.runLater {
                                        Controllers.getMainController().labelCurrentSR.text = sr.toString()
                                        Controllers.getMainController().labelWins.text = 0.toString()
                                        Controllers.getMainController().labelLosses.text = 0.toString()
                                    }
                                    firstRun = false
                                } else if (sr != prevSR) {
                                    val obj = WLTracker.updateSR(sr, prevSR)
                                    prevSR = sr
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
        val user32 = User32.INSTANCE
        val gdI32 = GDI32.INSTANCE
        val hdcWindow = user32.GetDC(hWnd)
        val hdcMemDC = gdI32.CreateCompatibleDC(hdcWindow)
        val bounds = WinDef.RECT()
        user32.GetClientRect(hWnd, bounds)
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (height == 0 || width == 0)
            return null
        val hBitmap = gdI32.CreateCompatibleBitmap(hdcWindow, width, height)
        val hOld = gdI32.SelectObject(hdcMemDC, hBitmap)
        gdI32.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, GDI32.SRCCOPY)
        gdI32.SelectObject(hdcMemDC, hOld)
        gdI32.DeleteDC(hdcMemDC)
        val bmi = WinGDI.BITMAPINFO()
        bmi.bmiHeader.biWidth = width
        bmi.bmiHeader.biHeight = -height
        bmi.bmiHeader.biPlanes = 1
        bmi.bmiHeader.biBitCount = 32
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB
        val buffer = Memory((width * height * 4).toLong())
        gdI32.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width)
        gdI32.DeleteObject(hBitmap)
        user32.ReleaseDC(hWnd, hdcWindow)
        return image
    }
}