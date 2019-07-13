package pro.eugw.owstreamrecord

import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Palette
import java.awt.image.BufferedImage
import kotlin.math.abs

fun String.isNumber(): Boolean {
    toCharArray().forEach { ch ->
        if (!ch.isDigit())
            return false
    }
    return true
}

fun nearColorCompare(color1: Int, color2: Int, allowableDifference: Int) = nearColorCompare(color1, color2, allowableDifference, false)

fun nearColorCompare(color1: Int, color2: Int, allowableDifference: Int, debug: Boolean): Boolean {
    val red1 = color1 shr 16 and 0xFF
    val green1 = color1 shr 8 and 0xFF
    val blue1 = color1 and 0xFF
    val red2 = color2 shr 16 and 0xFF
    val green2 = color2 shr 8 and 0xFF
    val blue2 = color2 and 0xFF
    if (debug) {
        println("r1:$red1 - r2:$red2")
        println("r1:$green1 - r2:$green2")
        println("r1:$blue1 - r2:$blue2")
        println(((abs(red2 - red1) <= allowableDifference && abs(green2 - green1) <= allowableDifference && abs(blue2 - blue1) <= allowableDifference)))
    }
    return ((abs(red2 - red1) <= allowableDifference && abs(green2 - green1) <= allowableDifference && abs(blue2 - blue1) <= allowableDifference))
}

fun analyzeColors(image: BufferedImage): Int {
    val builder = Palette.Builder(Bitmap(image))
    builder.maximumColorCount(2)
    return builder.generate().getDominantColor(image.getRGB(0, 0))
}

