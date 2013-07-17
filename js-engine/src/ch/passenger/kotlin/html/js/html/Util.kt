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
    public var target : HTMLElement

    fun targetId() {
        target.id
    }

    fun preventDefault() = js.noImpl
}

public trait Callback {
    fun callback(event : DOMEvent)
}

public trait Observer<T> {
    fun add(t:T)
    fun load(t:T)
    fun remove(t:T)
    fun delete(t:T)
    fun update(t:T,prop:String,old:Any?,nv:Any?)
}

public trait Observable<T> {
    val observers : Set<Observer<T>>

    fun add(t:T) {
        for(o in observers) o.add(t)
    }
    fun load(t:T) {
        for(o in observers) o.load(t)
    }
    fun remove(t:T){
        for(o in observers) o.remove(t)
    }
    fun delete(t:T) {
        for(o in observers) o.delete(t)
    }
    fun update(t:T,prop:String,old:Any?,nv:Any?) {
        for(o in observers) o.update(t, prop, old, nv)
    }
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
