package ch.passenger.kotlin.symbolon

import java.util.ArrayList
import java.util.HashMap
import java.util.TreeSet
import java.util.regex.Pattern

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import ch.passenger.kotlin.basis.Identifiable
import ch.passenger.kotlin.basis.Named
import kotlin.properties.Delegates
import ch.passenger.kotlin.basis.Event
import ch.passenger.kotlin.basis.Observable
import ch.passenger.kotlin.basis.Observer
import java.util.HashSet
import ch.passenger.kotlin.basis.EventTypes
import ch.passenger.kotlin.basis.ElementProducer
import ch.passenger.kotlin.basis.Filter
import ch.passenger.kotlin.basis.Interest
import ch.passenger.kotlin.basis.UpdateEvent
import ch.passenger.kotlin.basis.IdentityFilter
import java.net.URL
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.JsonNode

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 10:19
 * To change this template use File | Settings | File Templates.
 */

object NothingTypeWord : TypeWord("NONE", -1.toLong())

public open class Word(override val name : String, override val id : Long) : Comparable<Word>, Identifiable, Named, Observable<Word> {
    public override val observers: MutableSet<Observer<Word>> = HashSet()
    public var kind : TypeWord by Delegates.observable(NothingTypeWord, propertyHandler())
    public var description : String by Delegates.observable("", propertyHandler())
    val qualities : MutableSet<Word> by Delegates.observable(HashSet<Word>(), propertyHandler())


    public override fun compareTo(other: Word): Int {
        return name.compareTo(other.name)
    }

    fun<T> propertyHandler() : (PropertyMetadata, T, T) -> Unit {
        return {
            desc, old, new ->
            println("${desc.name}: ${old} -> ${new}")

            produce(UpdateEvent(this, desc, old, new))
        }
    }

}



fun wlist2name(wl : List<Constituent>) : String {
    val sb  = StringBuilder()
    wl.forEach {
        if(sb.length()>0) sb.append(' ')
        sb.append(it)
    }
    return sb.toString()
}

public open class TypeWord(name :String, id : Long) : Word(name, id)

class Constituent(val w : Word, val role : Word) {
    fun toString() : String {
        return "${w.name}[${role.name}]"
    }
}

public class Sentence(val words: List<Constituent>, id: Long) : Word(wlist2name(words), id) {

}

public object Universe : Observable<Word>, ElementProducer<Word> {
    public override val observers: MutableSet<Observer<Word>> = HashSet()
    val dictionary : MutableMap<Long,Word> = HashMap()
    var nextId : Long = 1.toLong()


    fun id() : Long {
        while (dictionary[nextId]!=null) {
            nextId++
        }
        return nextId;
    }

    fun add(w : Word) {
        dictionary[w.id] = w
        produce(Event(w, EventTypes.CREATE))
    }

    fun remove(w : Word) : Word? {
        val res = dictionary.remove(w.id)
        if(res==null) return null
        produce(Event(w, EventTypes.DELETE))
        return res
    }

    fun find(regex : String) : Set<Word> {
        val p = Pattern.compile(regex)
        val r = TreeSet<Word>()

        dictionary.values().forEach {
            if(p?.matcher(it.name)?.matches()!!)
                r.add(it)
        }

        return r
    }

    fun iterate(): Iterator<Word> {
        return dictionary.values().iterator()
    }


    override fun produce(f: Filter<Word>): Iterator<Word> {
        return iterate().filter { f.accept(it) }
    }
}

open class HRefWord(name : String, id : Long, val href : String, var mime : String) : Word(name, id)
class WikiWord(name : String, id : Long, href : String, val pageId : Long) : HRefWord(name, id, href, "text/html")



class WordInterest(override val producer : ElementProducer<Word>) : Interest<Word> {
    private val words : MutableMap<Long,Word> = HashMap()
    public override val observers: MutableSet<Observer<Word>> = HashSet()
    override var filter: Filter<Word> = IdentityFilter()
    override fun elements(): Iterable<Word> {
        return words.values()
    }

    override fun addHook(e: Word): Word? {
        if(words[e.id]!=null) return words[e.id]
        words[e.id] = e
        return e
    }

    override fun removeHook(e: Word): Word? {
        return words.remove(e.id)
    }
}

class TestObserver : Observer<Word> {
    override fun consume(e: Event<Word>) {
        println("${e.kind}: ${e.source.id} ${e.source.name}")
        if(e is UpdateEvent<*,*>)
            println("update: ${e.old} -> ${e.new}")
        if(e.source is WikiWord) {
            val w = e.source as WikiWord
            println("Wiki: ${w.name} url = ${w.href}")
        }

    }
}


fun main(args: Array<String>): Unit {
    Populator.populate()
    val interest = WordInterest(Universe)

    interest.addObserver(TestObserver())
    interest.init()

    val set = Universe.find("a")
    set.forEach {
        it.description = it.name
    }

    val query = "http://it.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=calvino&format=json&gsrprop=snippet&prop=info&inprop=url"
    val url :URL  = URL(query)
    val inputStream = url.openStream()
    val reader = inputStream?.reader(defaultCharset)
    val om = ObjectMapper()
    val jsonNode = om.readTree(reader)
    val valueAsString = om.writerWithDefaultPrettyPrinter()?.writeValueAsString(jsonNode)
    println(valueAsString)
    val pages = jsonNode?.path("query")?.path("pages")
    for(f in pages?.fieldNames()) {
        val p = pages?.get(f)
        val pid = p?.get("pageid")?.asLong()
        val w = WikiWord(p?.get("title")?.textValue()!!, Universe.id(), p?.get("fullurl")?.textValue()!!, pid!!)
        Universe.add(w)
    }
}


