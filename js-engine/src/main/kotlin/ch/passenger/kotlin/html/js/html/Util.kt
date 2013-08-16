package ch.passenger.kotlin.html.js.html

import js.dom.html.HTMLElement
import js.jquery.JQuery
import java.util.HashMap
import js.jquery.jq
import ch.passenger.kotlin.html.js.Session

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 11.07.13
 * Time: 13:42
 * To change this template use File | Settings | File Templates.
 */
public native fun JQuery.`val`(v: String?): JQuery = js.noImpl
public native fun JQuery.on(types : String, selector : String, cb : (event : DOMEvent) -> Unit): Unit = js.noImpl
public native fun JQuery.data(name: String,value : Any?): Unit = js.noImpl
public native fun JQuery.data(name: String): Any? = js.noImpl
public native fun JQuery.removeData(name: String): Unit = js.noImpl
public native fun JQuery.replaceWith(html: String?): JQuery = js.noImpl
public native fun JQuery.has(selector: String?): JQuery = js.noImpl

public native trait DOMEvent {
    public val target : HTMLElement
    public val data : Any?

    fun targetId() {
        target.id
    }

    fun preventDefault() = js.noImpl
}



public trait Callback {
    fun callback(event : DOMEvent)
}


public class ActionHolder {
    private val actions : MutableMap<Int,Callback> = HashMap()
    private var id : Int = 0

    fun add(cb: Callback) : Int {
        id = id+1
        actions.put(id, cb)
        return id
    }

    fun remove(id : Long) {
        actions.remove(id)
    }

    fun trigger(e : DOMEvent) {
        val said = jq(e.target).data("action").toString()
        val aid = safeParseInt(said)

        actions.get(aid)?.callback(e)
    }
}

public native trait MyWindow {
    public native var bosork : Session?
}
