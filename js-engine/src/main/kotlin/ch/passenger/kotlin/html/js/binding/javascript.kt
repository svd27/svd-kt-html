package ch.passenger.kotlin.html.js.binding

native trait Self {
    native fun setInterval(cb:()->Unit, delay:Long, vararg params:Any?) : Any
    native fun setTimeout(cb:()->Unit, delay:Long, vararg params:Any?) : Any
    native fun clearTimeout(id:Any)
    native fun clearInterval(id:Any)
}

/**
 * Created by sdju on 26.08.13.
 */
native class Date(epoch:Long) {
    native fun parse(ds:String) : Long = js.noImpl
    native fun UTC(year:Int, month:Int, date:Int, hrs:Int=0, min:Int=0, sec:Int=0, ms:Int=0) : Long = js.noImpl
    native fun getDate() : Int = js.noImpl
    native fun getDay(): Int = js.noImpl
    native fun getFullYear(): Int = js.noImpl
    native fun getHours(): Int = js.noImpl
    native fun getMilliseconds(): Int = js.noImpl
    native fun getMinutes(): Int = js.noImpl
    native fun getMonth(): Int = js.noImpl
    native fun getSeconds() : Int = js.noImpl
    native fun getTime(): Long = js.noImpl
    native fun getTimezoneOffset(): Int = js.noImpl
    native fun getUTCDate() : Int = js.noImpl
    native fun getUTCDay(): Int = js.noImpl
    native fun getUTCFullYear(): Int = js.noImpl
    native fun getUTCHours(): Int = js.noImpl
    native fun getUTCMilliseconds(): Int = js.noImpl
    native fun getUTCMinutes(): Int = js.noImpl
    native fun getUTCMonth(): Int = js.noImpl
    native fun getUTCSeconds() : Int = js.noImpl
    native fun setDate(v:Int)  = js.noImpl
    native fun setDay(v:Int) = js.noImpl
    native fun setFullYear(v:Int) = js.noImpl
    native fun setHours(v:Int) = js.noImpl
    native fun setMilliseconds(v:Int) = js.noImpl
    native fun setMinutes(v:Int) = js.noImpl
    native fun setMonth(v:Int) = js.noImpl
    native fun setSeconds(v:Int)  = js.noImpl
    native fun setTime(v:Int): Long = js.noImpl
    native fun setTimezoneOffset(v:Int) = js.noImpl

    class object {
        native fun now() : Long = js.noImpl
    }
}