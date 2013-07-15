package ch.passenger.kotlin.basis

import kotlin.properties.Delegates
import java.util.concurrent.ArrayBlockingQueue
import java.util.Queue
import java.util.HashSet

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 13:02
 * To change this template use File | Settings | File Templates.
 */

trait Identifiable {
    val id : Long
}

trait Named {
    val name : String
}

trait Interesting<T : Identifiable> {
    val container : InterestManager<T>
}


enum class EventTypes {
    CREATE ADD LOAD UPDATE REMOVE DELETE OTHER PAGE
}

abstract class Event<T>

open public class ElementEvent<T : Identifiable>(public val source : T, public val kind : EventTypes) : Event<T>()
class UpdateEvent<T : Identifiable,P>(source : T, val p : jet.PropertyMetadata, val old : P, val new : P)
: ElementEvent<T>(source, EventTypes.UPDATE)


open class InterestEvent<T : Identifiable>(val source : Interest<T>, val elements : Iterable<T>, kind : EventTypes) : Event<T>()
class LoadEvent<T : Identifiable>(source : Interest<T>, elements : Iterable<T>) : InterestEvent<T>(source, elements, EventTypes.LOAD)
class PageEvent<T : Identifiable>(source : Interest<T>, page : Iterable<T>, val paged : Paged<T>) : InterestEvent<T>(source, page, EventTypes.PAGE)

trait Observer<T : Identifiable> {
    open fun accept(et : EventTypes) : Boolean {
        return true
    }
    fun consume(e: ElementEvent<T>)
    fun consume(e: InterestEvent<T>)
}

trait Observable<T : Identifiable> {
    public val observers : MutableSet<Observer<T>>
    protected fun produce(event : ElementEvent<T>) {
        for(o in observers) o.consume(event)
    }

    protected fun produce(event : InterestEvent<T>) {
        for(o in observers) o.consume(event)
    }

    fun addObserver(o : Observer<T>) {
        observers.add(o)
    }

    fun removeObserver(o : Observer<T>) {
        observers.remove(o)
    }
}

trait Filter<T> {
    fun accept(t : T) : Boolean
}

trait ElementProducer<T : Identifiable> : Observable<T>  {
    fun produce(f : Filter<T>) : Iterator<T>
}

trait Sorter<T> {
    fun sort(source: Iterable<T>) : Iterable<T>
}

class IdentityFilter<T> : Filter<T> {
    override fun accept(t: T): Boolean {
        return true
    }
}

val evts = array(EventTypes.ADD, EventTypes.CREATE, EventTypes.DELETE, EventTypes.REMOVE, EventTypes.UPDATE)

trait Paged<T : Identifiable> : Observable<T>, Observer<T> {
    private var rowsPerPage : Int
    private var pages : Int
    private var current : Int
    private var page : Array<T>

    fun elements() : Iterable<T>

    override fun accept(et: EventTypes): Boolean {
        return evts.any { et.equals(it) }
    }

    private fun update() {

    }
}

trait Interest<T : Identifiable> : Paged<T>, Observer<T>, Identifiable {
    val producer : ElementProducer<T>
    var filter : Filter<T>
    val paged : Paged<T>

    fun addHook(e :T) : T?

    fun removeHook(e :T) : T?


    fun init() {
        producer.produce(filter).forEach {
            addHook(it)
        }
        if(elements().count()>0) {
            produce(LoadEvent(this, elements()))
        }
        producer.addObserver(this)
    }


    override fun consume(e: ElementEvent<T>) {
        when(e.kind) {
            EventTypes.CREATE -> {
                produce(e)
                add(e.source)
            }
            EventTypes.DELETE -> {
                remove(e.source)
                produce(e)
            }
            EventTypes.UPDATE -> {
                if(elements().containsItem(e.source)) {
                    produce(e)
                    if(!filter.accept(e.source)) {
                        remove(e.source)
                    }
                } else {
                    if (filter.accept(e.source)) add(e.source)
                }
            }
            else -> throw IllegalStateException()
        }
    }

    fun add(t : T) {
        if(!elements().containsItem(t)) {
            val ae = addHook(t)
            if(ae !=null) {
                val theT : T = ae
                produce(ElementEvent(theT, EventTypes.ADD))
            }
            else throw IllegalStateException()
        }
    }

    fun remove(t : T) {
        if(elements().containsItem(t)) {
            val re = removeHook(t)
            if(re !=null) {
                val theT : T = re
                produce(ElementEvent(theT, EventTypes.LOAD))
            }
            else throw IllegalStateException()
        }
    }


}

trait InterestManager<T : Identifiable> : Observable<T> {
    fun updated(event : UpdateEvent<T,*>) {
        produce(event)
    }
}


