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

    private var firstRun = true
    private var resetted = true
    private var groupMode = false

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
        while (true) {
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
            }, null)
            if (window != null) {
                val img = capture(window as WinDef.HWND)
                Platform.runLater { Controllers.getMainController().imageViewOWPreview.image = SwingFXUtils.toFXImage(img, null) }
                if (img != null) {
                    resetted = false
                    var largeMode = true
                    if (!nearColorCompare(img.getRGB(970, 545), img.getRGB(970, 500), 5))
                        largeMode = false
                    var subImg = if (largeMode) img.getSubimage(1100, 500 - if (groupMode) 54 else 0, 100, 45) else img.getSubimage(1075, 512 - if (groupMode) 40 else 0, 97, 37)
                    subImg = postProc(subImg, analyzeColors(subImg))
                    val h = subImg.height
                    val w = subImg.width
                    if (nearColorCompare(subImg.getRGB(0,0), subImg.getRGB(w - 1, h - 1), 5) && nearColorCompare(img.getRGB(980, 770), img.getRGB(1275, 770), 5)) {
                        val ocr = tess.doOCR(subImg).substringBefore("\n")
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
                                    File(ConfigController.getConfig()["outputPath"].asString).writeText(ConfigController.getConfig()["outputTemplate"].asString.replace("%w", 0.toString()).replace("%l", 0.toString()).replace("%sr", sr.toString()))
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
                        } else groupMode = !groupMode
                    }
                }
            } else if (ConfigController.getConfig()["resetWLSR"].asBoolean && !resetted) {
                resetted = true
                firstRun = true
                prevSR = 0
                Platform.runLater {
                    Controllers.getMainController().labelCurrentSR.text = ""
                    Controllers.getMainController().labelWins.text = 0.toString()
                    Controllers.getMainController().labelLosses.text = 0.toString()
                }
            }
            sleep(ConfigController.getConfig().get("period").asLong)
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

    private fun postProc(origin: BufferedImage, color: Int): BufferedImage {
        for (i in 0 until origin.width) {
            for (j in 0 until origin.height) {
                if (nearColorCompare(origin.getRGB(i, j), color, 30)) {
                    origin.setRGB(i, j, 0)
                } else {
                    origin.setRGB(i, j, 16777215)
                }
            }
        }
        return origin
    }

}