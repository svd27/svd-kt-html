package ch.passenger.kotlin.basis

import kotlin.properties.Delegates
import java.util.concurrent.ArrayBlockingQueue
import java.util.Queue
import java.util.HashSet
import java.util.concurrent.BlockingQueue
import java.util.ArrayList
import java.net.URI
import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 13:02
 * To change this template use File | Settings | File Templates.
 */




trait Identifiable {
    val id : URN
}

trait Named {
    val name : String
}

trait Committable {
    fun commit() : Unit
}

class CommitException(msg : String?, cause : Throwable?, enableSuppression:Boolean=false, writableStackTrace:Boolean=true) : Exception(msg, cause, enableSuppression, writableStackTrace)

trait Workable  {
    fun onSuccess() {}
    fun onFailure(e: Exception) {}
}

fun<T : Committable, W : Workable> T.work(c:T, w : W, f: T.() -> Unit) {
    var failed = false
    try {
        c.f()
    } catch (e:Exception) {
        w.onFailure(e)
    } finally {
        c.commit()
    }
    w.onSuccess()
}

trait Versioned : Committable {
    var version : Long
}


enum class EventTypes {
    CREATE ADD LOAD UPDATE REMOVE DELETE OTHER PAGE
}

abstract class Event<T>(val kind : EventTypes)

open public class ElementEvent<T : Identifiable>(public val source : T, kind : EventTypes) : Event<T>(kind)
class UpdateEvent<T : Identifiable,P>(source : T, val p : jet.PropertyMetadata, val old : P, val new : P)
: ElementEvent<T>(source, EventTypes.UPDATE)


open class InterestEvent<T : Identifiable>(val source : Interest<T>, val elements : Iterable<T>, kind : EventTypes) : Event<T>(kind)
class LoadEvent<T : Identifiable>(source : Interest<T>, elements : List<T>) : InterestEvent<T>(source, elements, EventTypes.LOAD)
class PageEvent<T : Identifiable>(public val paged : Paged<T>, public val page : Page<T>) : Event<T>(EventTypes.PAGE)

trait Observer<T : Identifiable> {
    open fun accept(et : EventTypes) : Boolean {
        return true
    }
    fun consume(e: Event<T>)
}

trait Observable<T : Identifiable> {
    protected val observers : MutableSet<Observer<T>>

    protected fun preProduce(e: Event<T> ) {}
    protected fun postProduce(e: Event<T>) {}

    protected fun produce(event : Event<T>) {
        preProduce(event)
        for(o in observers) o.consume(event)
        postProduce(event)
    }

    fun addObserver(o : Observer<T>) {
        observers.add(o)
    }

    fun removeObserver(o : Observer<T>) {
        observers.remove(o)
    }
}



public abstract class EventQueue<T:Identifiable, E: Event<T>>(val q : BlockingQueue<E>) : BlockingQueue<E> by q

fun main(args : Array<String>) {
    val surn1 = URN.word()

    val urn1 = URN(surn1)

    println(urn1.urn)

    val matcher : Matcher = urn1.matcher()
    val b = matcher.matches()
    for(i in 0..matcher.groupCount())
        println("$i: ${matcher.group(i)}")

}
