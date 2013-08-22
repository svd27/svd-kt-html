package ch.passenger.kotlin.html.js.model

import js.debug.console
import java.util.HashSet
import java.util.ArrayList
import ch.passenger.kotlin.html.js.html.each

/**
 * Created by sdju on 16.08.13.
 */

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
           }
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
            if(it is String) ""
            else if(it is Observable<*>) it.addObserver(this)
        }
    }

    open fun add(v:T) {
        if(items.add(v)) fireAdd(v)
    }

    open fun remove(v:T) {
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
        items.remove(t)
        fireRemove(t)
    }
    override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
        fireUpdate(t, prop, old, nv)
    }
}

trait SelectionModel<T,C:MutableCollection<T>> : CollectionModel<T,C> {
    val _selections : MutableSet<T>
    val selections : Set<T>
       get() = _selections
    val multi : Boolean

    fun deselect(sel:T) {
        if(selections.contains(sel)) {
            _selections.remove(sel)
            fireUnLoad(sel)
        }
    }

    fun select(sel:T) {
        if(selections.contains(sel)) return;
        if(!multi) _selections.clear()
        _selections.add(sel)
        fireLoad(sel)
    }
}

class DefaultObservable<T>() : Observable<T> {
    override val observers: MutableSet<Observer<T>> = HashSet()
}

abstract class AbstractSelectionModel<T>(val values : Iterable<T>, override val multi:Boolean) : SelectionModel<T,MutableList<T>>,
        Observable<T>  {
    override val observers: MutableSet<Observer<T>> = HashSet()
    override val _selections: MutableSet<T> = HashSet()
    override val items: MutableList<T> = ArrayList();

    {
        values.each {
            items.add(it)
        }
        init()
    }
}

class StringSelectionModel(values : Iterable<String>, multi:Boolean) : AbstractSelectionModel<String>(values, multi) {

}