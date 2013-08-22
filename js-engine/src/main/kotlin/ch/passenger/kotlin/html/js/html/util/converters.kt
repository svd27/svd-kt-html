package ch.passenger.kotlin.html.js.html.util

import js.dom.html.window
import ch.passenger.kotlin.html.js.html.MyWindow

/**
 * Created by sdju on 22.08.13.
 */
trait Converter<T> {
    fun convert2string(t:T) : String
    fun convert2target(s:String):T
}

abstract class NumberConverter<T:Number> : Converter<Number> {
    override fun convert2string(t: Number): String {
        return "$t"
    }
    fun crtNumber(s:String) :Double{
        val mw = window as MyWindow
        return mw.parseFloat(s)
    }
}

open class IntConverter : NumberConverter<Int>() {
    override fun convert2target(s: String): Number {
        return crtNumber(s).toInt()
    }
}

open class DoubleConverter : NumberConverter<Double>() {
    override fun convert2target(s: String): Number {
        return crtNumber(s).toDouble()
    }
}

class BooleanConverter : Converter<Boolean> {

    override fun convert2string(t: Boolean): String {
        return "$t"
    }
    override fun convert2target(s: String): Boolean {
        if(s=="true") return true
        return false
    }
}
