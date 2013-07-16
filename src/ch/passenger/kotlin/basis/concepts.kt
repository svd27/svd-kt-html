package ch.passenger.kotlin.basis

import kotlin.properties.Delegates
import java.util.concurrent.ArrayBlockingQueue
import java.util.Queue
import java.util.HashSet
import java.util.concurrent.BlockingQueue
import java.util.ArrayList

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

trait Filter<T> {
    fun accept(t : T) : Boolean
}

trait ElementProducer<T : Identifiable> : Observable<T>  {
    fun produce(f : Filter<T>) : Iterator<T>
    fun retrieve(vararg id : Long) : Iterable<T>
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

trait Container<T : Identifiable> {
    public val elements : MutableList<T>
}

class Page<T : Identifiable>(val elements : List<T>, public val totalPages : Int, val rowsPerPage : Int, val totalRows : Int, val first : Int, val last : Int)

trait Paged<T : Identifiable> : Observable<T>, Container<T>, Identifiable, Named {
    public var rowsPerPage : Int
    public var current : Int
    public var page : Page<T>

    protected fun pageInit() {
        if(rowsPerPage < 0 || elements.size() < rowsPerPage) {
            page = Page(elements, 1, rowsPerPage, elements.size, 0, elements.size - 1)
            produce(PageEvent(this, page))
        } else {
            val total = if(rowsPerPage == 0) 0 else (elements.size / rowsPerPage)
            if(current >= total) current = Math.max(0, total - 1)
            val start = current * rowsPerPage
            val end = Math.min(elements.size - 1, start + rowsPerPage)
            page = Page(elements.subList(start, end), current, rowsPerPage, elements.size, start, end)
            produce(PageEvent(this, page))
        }
    }

    public fun config(rpp : Int) {
        if(rpp != rowsPerPage) {
            rowsPerPage = rpp
            pageInit()
        }
    }


    private fun calcCurrentAndTotal() : Pair<Int,Int> {
        val total = if(rowsPerPage == 0) 0 else (elements.size / rowsPerPage)
        var c : Int = 0
        if(current >= total) c = Math.max(0, total - 1)
        return Pair(total, c)
    }

    protected fun pageUpdate(e : ElementEvent<T>) {
        if(rowsPerPage<=0)
            pageInit()
        else {
            val res = calcCurrentAndTotal()
            if(res.second!=current) {
                pageInit()
            }
            val start = current * rowsPerPage
            val end = Math.min(elements.size - 1, start + rowsPerPage)
            val np = Page(elements.subList(start, end), current, rowsPerPage, elements.size, start, end)
            if(np.elements.size()!=page.elements.size()) {
                pageInit()
                return
            }
            for(i in 0..(np.elements.size()-1)) {
                if(np.elements[i]!=page.elements[i]) {
                    pageInit()
                    return
                }
            }
        }
    }

    public fun next() {
        val pair = calcCurrentAndTotal()
        if(current+1<pair.first) {
            current++
            pageInit()
        }
    }

    public fun prev() {
        if(current>0) {
            current--
            pageInit()
        }
    }
}


trait Interest<T : Identifiable> : Paged<T>, Observer<T>, Identifiable,Named {
    val producer : ElementProducer<T>
    var filter : Filter<T>

    fun addHook(e :T) : T?

    fun removeHook(e :T) : T?

    override fun accept(et: EventTypes): Boolean {
        return evts.any { et.equals(it) }
    }

    fun preInit() {}
    fun postInit() {}

    fun init() {
        preInit()
        producer.produce(filter).forEach {
            addHook(it)
        }

        produce(InterestEvent(this, elements, EventTypes.LOAD))
        pageInit()
        producer.addObserver(this)
        postInit()
    }


    override fun consume(e : Event<T>) {
        when(e) {
            is ElementEvent<T> -> consumeEl(e)
            else -> return
        }
    }


    protected override fun postProduce(e: Event<T>) {
        if(e is ElementEvent<T>) pageUpdate(e)
    }

    private fun consumeEl(e: ElementEvent<T>) {
        when(e.kind) {
            EventTypes.CREATE -> {
                if(add(e.source)) produce(e)
            }
            EventTypes.DELETE -> {
                if(remove(e.source)) produce(e)
            }
            EventTypes.UPDATE -> {
                if(elements.containsItem(e.source)) {
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

    fun add(t : T) :Boolean {
        if(!elements.containsItem(t)) {
            val ae = addHook(t)
            if(ae !=null) {
                val theT : T = ae
                return true
            }
            else throw IllegalStateException()
        }
        return false
    }

    fun remove(t : T) : Boolean {
        if(elements.containsItem(t)) {
            val re = removeHook(t)
            if(re !=null) {
                val theT : T = re
                produce(ElementEvent(theT, EventTypes.REMOVE))
                return true
            }
            else throw IllegalStateException()
        }
        return false
    }
}

trait StaticInterest<T : Identifiable> : Interest<T> {
    override fun preInit() {
        filter = object : Filter<T> {
            override fun accept(t: T): Boolean {
                return false
            }
        }
    }
    override fun postInit() {
        filter = object : Filter<T> {
            override fun accept(t: T): Boolean {
                return elements.containsItem(t)
            }
        }
    }

    fun add(id : Long) {
        producer.retrieve(id)
    }
}


public abstract class EventQueue<T:Identifiable, E: Event<T>>(val q : BlockingQueue<E>) : BlockingQueue<E> by q


