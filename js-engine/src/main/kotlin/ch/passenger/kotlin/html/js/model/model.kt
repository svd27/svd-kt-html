package ch.passenger.kotlin.html.js.model

/**
 * Created by sdju on 16.08.13.
 */

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

trait Model<T> : Observable<T> {
    var t : T
}


class ValueHolder<T>(init:T) {
    var value : T = init
}

abstract class AbstractModel<T>(v:T) : Model<T> {
    val value : ValueHolder<T> = ValueHolder(v)
    override var t : T = v
    get() = $t
    set(v:T) {val ov = $t; $t = v; update(t, "", ov, v)}
}

trait CollectionModel<T,C:Collection<T>> : Observable<T> {
    val items : C
}