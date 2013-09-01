package ch.passenger.kotlin.html.js.model

/**
 * Created by Duric on 31.08.13.
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
