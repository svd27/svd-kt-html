package ch.passenger.kotlin.basis

import java.util.HashMap

/**
 * Created by sdju on 17.07.13.
 */
public object InterestManager {
    private val interests : MutableMap<URN,Interest<*>> = HashMap()

    fun createInterest(name : String, kind : URN, cfg : IntererstConfig) : URN {
        val interest = factories[kind]!!.create(name, cfg)
        interests[interest.id] = interest
        return interest.id
    }

    fun<T:Identifiable> get(id:URN) : Interest<T> = interests[id] as Interest<T>

    private val factories : MutableMap<URN,InterestFactory> = HashMap()

    public fun register(urn:URN, factory:InterestFactory) {
        factories[urn] = factory
    }

    public fun unregister(urn:URN, factory:InterestFactory) {
        factories.remove(urn)
    }
}


trait InterestFactory {
    fun accept(urn :URN):Boolean
    fun create(name : String, config : IntererstConfig) : Interest<*>
}


public trait IntererstConfig {
    val kind : URN
    val owner : URN
    val filter : FilterConfig
    val sorter : SortConfig
    val token : String
}

public trait FilterConfig {

}

public trait SortConfig {

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
