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

public class URN(val urn:String) :Comparable<URN> {
    private val pSchema = "symblicon|bosork"
    private val pDomConstituent = "[a-z][a-z0=9]*"
    private val pDomain = "$pDomConstituent\\.$pDomConstituent(\\.$pDomConstituent)+"
    private val pEntity = "(word)|(token)|(interest)";

    public val scheme : String
    public val thing : String
    public val domain : String
    public val specifier : String


    val pattern = Pattern.compile(
            //"urn:($pSchema):($pEntity):($pDomain):.*",
            "^urn:($pSchema):($pEntity):($pDomain):([a-z0-9()+,\\-\\.:=@;\$_!*']|%[0-9a-f]{2})+$",
            Pattern.CASE_INSENSITIVE);
    {
        val m = pattern.matcher(urn)
        if(!m.matches())
            throw IllegalStateException("bad urn $urn")
        scheme = m.group(1)!!
        thing = m.group(2)!!
        domain = m.group(6)!!
        specifier = m.group(8)!!
    }

    public fun matcher() : Matcher {
        return pattern.matcher(urn)
    }

    public override fun compareTo(other: URN): Int {
        return urn.compareToIgnoreCase(other.urn)
    }

    public fun equals(o: Any?) :Boolean {
        if(o is URN) return compareTo(o) == 0
        return false
    }

    class object {
        private var baseId : Long = 0
        fun word() : String {
            baseId = baseId+1
            return "urn:symblicon:word:root.symblicon.org:${baseId}"
        }

        private var baseToken : Long = 0
        fun token() : String {
            baseToken = baseToken+1
            return "urn:bosork:token:root.symblicon.org:${baseToken}"
        }

        private var baseInterest : Long = 0
        fun interest(token : String) : String {
            baseInterest = baseInterest+1
            return "urn:bosork:interest:root.symblicon.org:$token:${baseInterest}"
        }

    }
}



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

class Page<T : Identifiable>(val elements : List<T>, public val current : Int, public val totalPages : Int, val rowsPerPage : Int, val totalRows : Int, val first : Int, val last : Int)

trait Paged<T : Identifiable> : Observable<T>, Container<T>, Identifiable, Named {
    public var rowsPerPage : Int
    public var current : Int
    public var page : Page<T>

    protected fun pageInit() {
        if(rowsPerPage < 0 || elements.size() < rowsPerPage) {
            page = Page(elements, 1, 1, rowsPerPage, elements.size, 0, elements.size - 1)
            produce(PageEvent(this, page))
        } else {
            val res = calcCurrentAndTotal()
            val total = res.first
            //val total = if(rowsPerPage == 0) 0 else ((elements.size / rowsPerPage)+(if(elements.size() % rowsPerPage >0) 1 else 0))
            println("rows: ${elements.size()} rpp: ${rowsPerPage} total: ${total}")
            if(current >= total) current = Math.max(0, total - 1)
            val start = current * rowsPerPage
            val end = Math.min(elements.size - 1, start + rowsPerPage)
            page = Page(elements.subList(start, end), current, total, rowsPerPage, elements.size, start, end)
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
        val total = if(rowsPerPage == 0) 0 else (elements.size / rowsPerPage + (if(elements.size() % rowsPerPage >0) 1 else 0))
        var c : Int = 0
        if(current >= total) c = Math.max(0, total - 1) else c = current
        return Pair(total, c)
    }

    protected fun pageUpdate(e : ElementEvent<T>) {
        if(rowsPerPage<=0)
            pageInit()
        else {
            val res = calcCurrentAndTotal()
            current = res.second
            val start = current * rowsPerPage
            val end = Math.min(elements.size - 1, start + rowsPerPage)
            val np = Page(elements.subList(start, end), current, res.first, rowsPerPage, elements.size, start, end)
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

fun main(args : Array<String>) {
    val surn1 = URN.word()

    val urn1 = URN(surn1)

    println(urn1.urn)

    val matcher : Matcher = urn1.matcher()
    val b = matcher.matches()
    for(i in 0..matcher.groupCount())
        println("$i: ${matcher.group(i)}")

}
