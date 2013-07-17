package ch.passenger.kotlin.basis

import kotlin.properties.ReadWriteProperty

/**
 * Created by sdju on 17.07.13.
 */

public class VersionedProperty<P>(val initial : P) : ReadWriteProperty<Any?,P>  {
    var value : P = initial

    public override fun get(thisRef: Any?, desc: PropertyMetadata): P {
        return value
    }
    public override fun set(thisRef: Any?, desc: PropertyMetadata, v: P) {
        when(thisRef) {
            is Versioned -> version(thisRef, desc, v)
            else -> value = v

        }
    }

    fun version(thisRef:Versioned, desc: PropertyMetadata, v:P) {
        val old = value
        try {
            value = v
            thisRef.version +=1
            thisRef.commit()
        } catch (e:CommitException) {
            value = old
            thisRef.version -= 1
            throw e
        }
    }
}

public trait PropertyObserver<P> {
    fun before(ov:P, nv:P, desc:PropertyMetadata) : Boolean
    fun after(ov:P, nv:P, desc:PropertyMetadata)
}

public class ObservedProperty<P>(val delegate:ReadWriteProperty<Any?,P>, val observer : PropertyObserver<P>) : ReadWriteProperty<Any?,P> {

    public override fun get(thisRef: Any?, desc: PropertyMetadata): P {
        return delegate.get(thisRef, desc)
    }
    public override fun set(thisRef: Any?, desc: PropertyMetadata, value: P) {
        val ov = get(thisRef, desc)
        if(observer.before(ov, value, desc)) {
            delegate.set(thisRef, desc, value)
            observer.after(ov, get(thisRef, desc), desc)
        }
    }
}