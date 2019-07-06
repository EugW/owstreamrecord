package pro.eugw.owstreamrecord

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class ConfigController {
    companion object {
        private var config: JsonObject? = null
        fun setConfig(jsonObject: JsonObject) {
            config = jsonObject
        }
        fun getConfig() = config!!
        fun setProp(key: String, value: Any) {
            when (value) {
                is String -> config!!.addProperty(key, value)
                is Boolean -> config!!.addProperty(key, value)
                is Char -> config!!.addProperty(key, value)
                is Number -> config!!.addProperty(key, value)
            }
        }
        fun saveConfig() {
            val file = File("config.json")
            if (config != null) {
                file.writeText(config.toString())
            }
        }
    }

    init {
        val file = File("config.json")
        try {
            JsonParser().parse(file.readText()).asJsonObject
        } catch (e: Exception) {
            file.writeText(Main::class.java.getResource("/config.json").readText())
        }
        val jsonObject = JsonParser().parse(file.readText()).asJsonObject
        setConfig(jsonObject)
        println("Config initialized")
    }
}