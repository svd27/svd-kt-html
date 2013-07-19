package ch.passenger.kotlin.symbolon

import java.util.ArrayList
import java.util.HashMap
import java.util.TreeSet
import java.util.regex.Pattern

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import ch.passenger.kotlin.basis.Identifiable
import ch.passenger.kotlin.basis.Named
import kotlin.properties.Delegates
import ch.passenger.kotlin.basis.ElementEvent
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
import ch.passenger.kotlin.basis.Event
import ch.passenger.kotlin.basis.Page
import ch.passenger.kotlin.basis.Paged
import ch.passenger.kotlin.basis.PageEvent
import ch.passenger.kotlin.basis.InterestEvent
import ch.passenger.kotlin.basis.Workable
import ch.passenger.kotlin.basis.Committable
import ch.passenger.kotlin.basis.ObservedProperty
import ch.passenger.kotlin.basis.PropertyObserver
import ch.passenger.kotlin.basis.VersionedProperty
import java.net.URI
import ch.passenger.kotlin.basis.URN
import ch.passenger.kotlin.basis.InterestFactory
import ch.passenger.kotlin.basis.IntererstConfig

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 10:19
 * To change this template use File | Settings | File Templates.
 */

object NothingTypeWord : TypeWord("NONE", URN(URN.word()))

public open class Word(override val name : String, override val id : URN) : Comparable<Word>, Identifiable, Named, Observable<Word>,Committable {
    public override val observers: MutableSet<Observer<Word>> = HashSet()
    public var kind : TypeWord by ObservedProperty<TypeWord>(VersionedProperty(NothingTypeWord),PO())
    public var description : String by ObservedProperty<String>(VersionedProperty(""), PO())
    val qualities : MutableSet<Word> by ObservedProperty<MutableSet<Word>>(VersionedProperty(HashSet()), PO())


    public override fun compareTo(other: Word): Int {
        return name.compareTo(other.name)
    }

    protected inner class PO<P> : PropertyObserver<P> {
        override fun before(ov: P, nv: P, desc: PropertyMetadata): Boolean {
            return true
        }
        override fun after(ov: P, nv: P, desc: PropertyMetadata) {
            println("${desc.name}: ${ov} -> ${nv}")

            produce(UpdateEvent(this@Word, desc, ov, nv))
        }
    }



    override fun commit() {

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

public open class TypeWord(name :String, id : URN) : Word(name, id)

class Constituent(val w : Word, val role : Word) {
    fun toString() : String {
        return "${w.name}[${role.name}]"
    }
}

public class Sentence(val words: List<Constituent>, id: URN) : Word(wlist2name(words), id) {

}

public object Universe : Observable<Word>, ElementProducer<Word> {
    public override val observers: MutableSet<Observer<Word>> = HashSet()
    val dictionary : MutableMap<URN,Word> = HashMap()
    var nextId : Long = 1.toLong()


    fun id() : Long {
        while (dictionary[nextId]!=null) {
            nextId++
        }
        return nextId;
    }

    fun add(w : Word) {
        dictionary[w.id] = w
        produce(ElementEvent(w, EventTypes.CREATE))
    }

    fun remove(w : Word) : Word? {
        val res = dictionary.remove(w.id)
        if(res==null) return null
        produce(ElementEvent(w, EventTypes.DELETE))
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


    override fun retrieve(vararg id: Long): Iterable<Word> {
        val l : MutableList<Word> = ArrayList(id.size)

        id.forEach {
            if(dictionary.containsKey(id)) l.add(dictionary[it]!!)
        }

        return l
    }
}

open class HRefWord(name : String, id : URN, val href : String, var mime : String) : Word(name, id)
class WikiWord(name : String, id : URN, href : String, val pageId : Long) : HRefWord(name, id, href, "text/html")



class WordInterest(override val id: URN, override val name : String, override val producer : ElementProducer<Word>) :
Interest<Word>
 {
    private val words : MutableMap<URN,Word> = HashMap()
    public override val observers: MutableSet<Observer<Word>> = HashSet()
    override var filter: Filter<Word> = IdentityFilter()
    override var page : Page<Word> = Page<Word>(ArrayList(), 0, 0, 0, 0, 0, 0)
    public override val elements: MutableList<Word> public get() = ArrayList(words.values())
    public override var rowsPerPage: Int = 10

     override fun addHook(e: Word): Word? {
        if(words[e.id]!=null) return words[e.id]
        words[e.id] = e
        return e
    }

    override fun removeHook(e: Word): Word? {
        return words.remove(e.id)
    }


     private var _current = 0
     public override var current: Int
     public get() = _current
     public set(v) = _current = v
 }

class WordInterestFactory : InterestFactory {
    private val myUrn : URN = URN("urn:symblicon:word:intererst")

    override fun accept(urn: URN): Boolean {
        return urn.equals(myUrn)
    }
    override fun create(name: String, config: IntererstConfig) : WordInterest {
        return WordInterest(URN(URN.interest(config.token)), name, Universe)
    }
}

class TestObserver : Observer<Word> {
    var fire : Boolean = false
    override fun consume(e: Event<Word>) {
        when(e) {
            is ElementEvent<Word> -> {
                if (fire) {
                    println("${e.kind}: ${e.source.id} ${e.source.name}")
                    val ee = e as ElementEvent<Word>
                    if(ee is UpdateEvent<Word,*>)  {
                        val ue = e as UpdateEvent<Word,*>
                        println("${ue.old} -> ${ue.new}")
                    }
                    if(ee.source is WikiWord) {
                        val w = e.source as WikiWord
                        println("Wiki: ${w.name} url = ${w.href}")
                    }
                }
            }
            is PageEvent<Word> -> {
                if (fire) {
                    println("${e.kind}: rpp: ${e.paged.rowsPerPage} current: ${e.paged.current} total: ${e.page.totalPages} rows: ${e.page.totalRows}")
                    val page = e.page
                    for(w in page.elements) {
                        println("${w.name}")
                    }
                }
            }
            is InterestEvent<Word> -> {if(e.kind==EventTypes.LOAD) {
                println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                fire = true}
            }
            else -> ""
        }
    }
}


fun main(args: Array<String>): Unit {
    Populator.populate()
    val interest = WordInterest(URN(URN.interest("1")), "all", Universe)

    interest.addObserver(TestObserver())
    interest.init()

    val set = Universe.find("a")
    set.forEach {
        it.description = it.name
    }

    interest.config(15)

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
        val w = WikiWord(p?.get("title")?.textValue()!!, URN(URN.word()), p?.get("fullurl")?.textValue()!!, pid!!)
        Universe.add(w)
    }

    for(i in 0..interest.page.totalPages) {
        interest.next()
    }
}


