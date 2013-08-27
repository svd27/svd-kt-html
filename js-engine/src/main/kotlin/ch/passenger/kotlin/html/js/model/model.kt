package ch.passenger.kotlin.html.js.model

import js.debug.console
import java.util.HashSet
import java.util.ArrayList
import ch.passenger.kotlin.html.js.html.each
import ch.passenger.kotlin.html.js.logger.Logger

/**
 * Created by sdju on 16.08.13.
 */

val log : Logger = Logger.logger("model")

public trait Observer<T> {
    fun added(t:T)
    fun loaded(t:T)
    fun unloaded(t:T)
    fun removed(t:T)
    fun deleted(t:T)
    fun updated(t:T,prop:String,old:Any?,nv:Any?)
}

public abstract class AbstractObserver<T> : Observer<T> {

    override open fun added(t: T) {
    }
    override fun loaded(t: T) {
    }
    override fun unloaded(t: T) {
    }
    override fun removed(t: T) {
    }
    override fun deleted(t: T) {
    }
    override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
    }
}

public trait Observable<T> {
    protected val observers : MutableSet<Observer<T>>

    public fun addObserver(o:Observer<T>):Boolean = observers.add(o)
    public fun removeObserver(o:Observer<T>):Boolean = observers.remove(o)

    fun fireAdd(t:T) {
        for(o in observers) {
            console.log("telling $o about add $t")
            o.added(t)
        }
    }
    fun fireLoad(t:T) {
        for(o in observers) o.loaded(t)
    }
    fun fireUnLoad(t:T) {
        for(o in observers) o.unloaded(t)
    }
    fun fireRemove(t:T){
        for(o in observers) o.removed(t)
    }
    fun fireDelete(t:T) {
        for(o in observers) o.deleted(t)
    }
    fun fireUpdate(t:T,prop:String,old:Any?,nv:Any?) {
        for(o in observers) o.updated(t, prop, old, nv)
    }
}



trait Dirty {
    public var dirty : Boolean
    public fun dirty() : Boolean = dirty
}

trait Model<T> : Observable<T> {
    protected var _value : T?
    public var value : T?
       get() = _value
       set(v) {
           if(v!=_value) {
               val ov = _value
               _value=v
               _value = checkValue(v, ov)
           }
       }

    open protected fun checkValue(nv:T?, ov:T?) : T? {
        if(_value==null) {
            fireRemove(ov!!)
            fireDelete(ov)
        } else if(ov==null) {
            fireAdd(_value!!)
        } else {
            var source = _value
            if(_value==null ) source = ov
            fireUpdate(source!!, "this", ov, _value)
        }
        return nv
    }

    /**
     * tells the model that new content may be available
     * it is up to the model to react to this or not
     * default implementation does nothing
     */
    open public fun refresh() {

    }

}


class ValueHolder<T>(init:T?) {
    var value : T? = init

    fun toString() : String = value?.toString()?:""
}


trait CollectionModel<T,C:MutableCollection<T>> : Observable<T>,Observer<T> {
    val items : C

    protected fun init() {
        items.each {
            try {
                if(it is Observable<*>) it.addObserver(this)
            } catch(e: Exception) {
                log.debug("$it really didnt like me and wont let me observe", e)
            }
        }
    }

    /**
     * tells the model that new content may be available
     * it is up to the model to react to this or not
     * default implementation does nothing
     */
    open public fun refresh() {

    }

    open fun add(v:T) {
        val l = items.size()
        items.add(v)
        log.debug("adding  $v size ${l} -> ${items.size()}")
        if(l!=items.size()) fireAdd(v)
    }

    open fun remove(v:T) {
        log.debug("remove ", v)
        if(items.remove(v)) fireRemove(v)
    }


    override fun added(t: T) {
        add(t)
    }
    override fun loaded(t: T) {

    }

    override fun unloaded(t: T) {

    }
    override fun removed(t: T) {
        deleted(t)
    }
    override fun deleted(t: T) {
        log.debug("deleted ", t)
        items.remove(t)
        fireRemove(t)
    }
    override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
        log.debug("update ", t, " $prop: $old->$nv")
        fireUpdate(t, prop, old, nv)
    }
}

trait SelectionModel<T> : CollectionModel<T,MutableList<T>> {
    val _selections : MutableSet<T>
    val selections : Set<T>
       get() = _selections
    val multi : Boolean

    fun deselect(sel:T) {
        log.debug("deselect: ", sel)
        if(selections.contains(sel)) {
            _selections.remove(sel)
            fireUnLoad(sel)
        }
    }

    fun select(sel:T) {
        log.debug("select: ", sel)
        if(selections.contains(sel)) return;
        if(!multi) _selections.clear()
        _selections.add(sel)
        fireLoad(sel)
    }

    fun firstSelected() : T? {
        if(selections.size()>0)  return selections.iterator().next()
        return null
    }
}

class DefaultObservable<T>() : Observable<T> {
    override val observers: MutableSet<Observer<T>> = HashSet()
}

abstract class AbstractSelectionModel<T>(val values : Iterable<T>, override val multi:Boolean) : SelectionModel<T>,
        Observable<T>  {
    override val items: MutableList<T> = ArrayList()
    override val observers: MutableSet<Observer<T>> = HashSet()
    override val _selections: MutableSet<T> = HashSet();


    {
        values.each {
            items.add(it)
        }
        init()
    }
}

open class SelectionObservableAdapter<T>(val observable:Observable<T>, initial:Iterable<T>, multi:Boolean=false) : AbstractSelectionModel<T>(initial, multi) {
    val log :Logger = Logger.logger("SelectionObservableAdapter");
    {
        observable.addObserver(object:Observer<T> {

            override fun added(t: T) {
                log.debug("added ", t)
                add(t)
            }
            override fun loaded(t: T) {

            }
            override fun unloaded(t: T) {

            }
            override fun removed(t: T) {
                log.debug("remvoed ", t)
                remove(t)
            }
            override fun deleted(t: T) {

            }
            override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
                log.debug("updated ", t, " $prop: $old->$nv")
                fireUpdate(t, prop, old, nv)
            }
        })
    }
    class object {

    }
}

class SelectionModelAdapter<T,C:MutableCollection<T>>(val cm:CollectionModel<T,C>, multi:Boolean=false) :
SelectionObservableAdapter<T>(cm, cm.items, multi)

class StringSelectionModel(values : Iterable<String>, multi:Boolean) : AbstractSelectionModel<String>(values, multi) {
    override val items: MutableList<String> = ArrayList()
}
