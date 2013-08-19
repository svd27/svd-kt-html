package ch.passenger.kotlin.html.js.html

import js.dom.html.HTMLElement
import js.jquery.JQuery
import java.util.HashMap
import js.jquery.jq
import ch.passenger.kotlin.html.js.Session
import java.util.ArrayList
import js.dom.html.document
import js.debug.console

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 11.07.13
 * Time: 13:42
 * To change this template use File | Settings | File Templates.
 */
public native fun document.addEventListener(kind:String, cb : (e:DOMEvent)->Unit, f:Boolean) : Unit = js.noImpl

trait EventTarget {
    public native fun addEventListener(kind:String, cb : (e:DOMEvent)->Any?, f:Boolean) : Unit = js.noImpl
}

public native fun JQuery.`val`(v: String?): JQuery = js.noImpl
public native fun JQuery.on(types : String, cb : (event : DOMEvent) -> Unit): Unit = js.noImpl
public native fun JQuery.on(types : String, selector : String, cb : (event : DOMEvent) -> Unit): Unit = js.noImpl
public native fun JQuery.data(name: String,value : Any?): Unit = js.noImpl
public native fun JQuery.data(name: String): Any? = js.noImpl
public native fun JQuery.removeData(name: String): Unit = js.noImpl
public native fun JQuery.replaceWith(html: String?): JQuery = js.noImpl
public native fun JQuery.after(html: String?): JQuery = js.noImpl
public native fun JQuery.before(html: String?): JQuery = js.noImpl
public native fun JQuery.has(selector: String?): JQuery = js.noImpl
public native fun JQuery.text(): String? = js.noImpl
public native fun JQuery.`val`(): Any? = js.noImpl
public fun JQuery.value(): Any? = this.`val`()
public fun JQuery.value(v:String?): JQuery = this.`val`(v)
public native fun JQuery.mouseenter(cb : (event : DOMEvent) -> Unit): Unit = js.noImpl
public native fun JQuery.mouseleave(cb : (event : DOMEvent) -> Unit): Unit = js.noImpl

public native trait DOMEvent {
    public val target : HTMLElement
    public var data : Any?

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

    fun action(e : DOMEvent) {
        val said = jq(e.target).data("action").toString()
        val aid = safeParseInt(said)

        actions.get(aid)?.callback(e)
    }

    fun enter(e : DOMEvent) {
        val said = jq(e.target).data("enter-action").toString()
        val aid = safeParseInt(said)

        console.log("AH: resolved enter action: $aid")
        actions.get(aid)?.callback(e)
    }

    fun leave(e : DOMEvent) {
        val said = jq(e.target).data("leave-action").toString()
        val aid = safeParseInt(said)

        console.log("AH: resolved leave action: $aid")
        actions.get(aid)?.callback(e)
    }

}

public native trait MyWindow {
    public native var bosork : Session?
}

public fun<T> Iterable<T>.each(cb:(T)->Unit): Unit {
        for(e in this) cb(e)
}

public fun<T> List<T>.eachIdx(cb:(Int,T)->Unit): Unit {
    val l = this.size()-1
    for(i in 0..l) cb(i, get(i))
}

public fun<T> Array<T>.each(cb:(T)->Unit): Unit {
    for(e in this) cb(e)
}

public fun<T> listOf(vararg  t:T) : List<T> {
    val l = ArrayList<T>()
    t.each {
        l.add(it)
    }

    return l
}