package pro.eugw.owstreamrecord

fun String.isNumber(): Boolean {
    toCharArray().forEach { ch ->
        if (!ch.isDigit())
            return false
    }
    return true
}