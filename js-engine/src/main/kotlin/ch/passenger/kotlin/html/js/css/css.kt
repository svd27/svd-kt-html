package ch.passenger.kotlin.html.js.css

import ch.passenger.kotlin.html.js.html.svg.Length
import ch.passenger.kotlin.html.js.html.each
import java.util.StringBuilder
import java.util.HashSet
import ch.passenger.kotlin.html.js.logger.Logger
import ch.passenger.kotlin.html.js.logger.LogFactory

/**
 * Created by Duric on 25.08.13.
 */

enum class CSSAgents {
    webkit moz ms o
}

abstract class CSSProperty<T>(val name:String, val value:Array<T>) {
    val log = LogFactory.logger("css.property")
    val support : MutableSet<CSSAgents> = HashSet()

    abstract fun writeValue(t:T) : String
    fun write() : String {
        val sb = StringBuilder()
        sb.append(write(""))

        support.each {
            log.debug("write property support ${it.name()}")
            sb.append(write("-${it.name()}"))
        }
        return sb.toString()
    }

    fun write(prefix:String) : String {
        val sb = StringBuilder()
        value.each {
            sb.append("$prefix$name: ${writeValue(it)};")
        }
        return sb.toString()
    }
}


class CSSStringProperty(name:String,value:Array<String>) : CSSProperty<String>(name, value) {
    override fun writeValue(t: String): String = t
}

class CSSLengthProperty(name:String, value:Array<Length>) : CSSProperty<Length>(name, value) {
    override fun writeValue(v:Length): String= "${value.toString()}"
}