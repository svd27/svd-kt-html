package ch.passenger.kotlin.html.js.html

import java.util.ArrayList
import java.util.StringBuilder
import java.util.HashMap
import js.dom.html.Event
import js.jquery.jq
import ch.passenger.kotlin.html.js.Session
import js.debug.console
import js.dom.html.window
import ch.passenger.kotlin.html.js.model.SelectionModel
import ch.passenger.kotlin.html.js.model.AbstractObserver
import ch.passenger.kotlin.html.js.model.Dirty
import js.dom.html.HTMLSelectElement
import js.dom.html.HTMLOptionElement
import js.dom.html.document
import ch.passenger.kotlin.html.js.html.svg.SVG
import ch.passenger.kotlin.html.js.html.svg.Extension
import ch.passenger.kotlin.html.js.html.svg.Length
import js.dom.core.Node
import js.dom.core.Attr
import js.dom.core.Element
import js.dom.core.TypeInfo
import java.util.HashSet
import ch.passenger.kotlin.html.js.model.Model
import ch.passenger.kotlin.html.js.model.Observer

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 08.07.13
 * Time: 19:14
 * To change this template use File | Settings | File Templates.
 */

fun session() : Session{
    val mw = window as MyWindow
    return mw.bosork!!
}

open class Attribute(val name:String, val value:String) {
    public fun render() : String {
        return "${name} = \"${value}\""
    }
}

public val ROOT_PARENT : HtmlElement = Text("")

abstract class HtmlElement(aid : String?) : Dirty {
    var parent : HtmlElement? = null
    var node : Node? = null
    var hidden : Boolean = false
    set(v) {
       if(v!=hidden) {
           $hidden=v
           dirty = true
           if(hidden) detach() else createNode()
       }
    }

    public override var dirty: Boolean = true
    set(v) {
        $dirty = v
        if(dirty) {
            var desc = "$this"
            if(this is Tag) desc = this.name
            console.log("$desc ${this.id()} wants a refresh")
            val SESSION = (window as MyWindow)!!.bosork!!
            SESSION.refresh(this)
        }
    }
    private val _children : MutableList<HtmlElement> = ArrayList<HtmlElement>()
    protected val children : List<HtmlElement>
    get() = _children

    public val tid : String = forceId(aid)
    public abstract fun createNode() : Node?

    public fun each(cb: (el:HtmlElement) -> Unit) {
        children.each { cb(it) }
    }

    public fun id() : String = tid

    abstract fun doRefresh()
    fun refresh() {
        dirty = false
        doRefresh()
    }

    protected fun addChild(e : HtmlElement) {
        e.parent = this
        console.log("adding: ", e)
        _children.add(e)
    }

    fun find(id:String) : HtmlElement? {
        var result : HtmlElement? = null
        if(id==id()) return this
        each {
            result = it.find(id)
        }

        return result
    }


    public fun precedingSibling(e:HtmlElement) : HtmlElement? {
       val idx = indexOf(e)
        if(idx==0) return null
        if(idx>0) return children[idx-1]
        var res : HtmlElement? = null
        each {
            val s = it.precedingSibling(e)
            if(s!=null) res = s
        }

        return res
    }

    public fun nextSibling(e:HtmlElement) : HtmlElement? {
        val idx = indexOf(e)
        if(idx==0) return if(children.size()>1) children[1] else null
        if(idx>0) return if(children.size()>idx+1) children[idx+1] else null
        var res : HtmlElement? = null
        each {
            val s = it.nextSibling(e)
            if(s!=null) res = s
        }

        return res
    }

    fun indexOf(e:HtmlElement) : Int {
        var idx = -1
        children.eachIdx {
            (i,c) -> if(c.id()==e.id()) idx = i
        }
        return idx
    }

    fun detach() {
        if (node!=null) {
            if(parent!=null && parent?.node!=null) {
                parent?.node?.removeChild(node!!)
            }
            node = null
            each { it.detach() }
        }
    }

    public fun clear(refresh:Boolean=false) {
        val cl = ArrayList<HtmlElement>()
        cl.addAll(children)
        cl.each { remove(it) }
        if(refresh) dirty = true
    }

    protected fun remove(c:HtmlElement) {
        val idx = indexOf(c)
        if(idx >=0) {
            c.clear()
            c.detach()

            //should have no more references to DOM after detaching all
            _children.remove(idx)

        }
    }

    fun insertIntoParent() {
        var sib = precedingSibling(this)
        var preceed = true
        if(sib==null) {
            preceed=false
            sib = nextSibling(this)
        }


        if(sib!=null && sib?.node!=null) {
            if(preceed) {
                parent?.node?.insertBefore(node!!,sib?.node!!)
            } else {
                if(sib?.node?.nextSibling!=null) {
                    parent?.node?.insertBefore(node!!,sib?.node!!.nextSibling!!)
                } else {
                    parent?.node?.appendChild(node!!)
                }
            }
        } else {
            parent?.node?.appendChild(node!!)
        }
    }
}

class Text(initial : String) : HtmlElement(null) {
    var content : String = initial
    set(s) {if(content!=s) {$content=s; dirty = true}}

    public override fun createNode() : Node?{
        if(hidden) return null
        if(parent!=null && (parent?.node!=null||parent==ROOT_PARENT)) {
            node = window.document.createTextNode(content)!!
            if(parent!=ROOT_PARENT) insertIntoParent()
        }
        return node
    }


    override fun doRefresh() {
        if(parent!=null && parent?.node!=null) {
            parent?.node?.textContent = content
        }
    }
}



class AttributeList(private val list : MutableMap<String,Attribute>) {
    fun att(name : String, value : String) {
        list.put(name, Attribute(name, value))
    }

    fun values(): MutableCollection<Attribute> {
        return list.values()
    }

    fun att(name : String) : Attribute? {
        return list.get(name)
    }

    fun contains(name : String) : Boolean {
        return list.containsKey(name)
    }

    fun remove(name:String) {
        list.remove(name)
    }

    fun refresh(n:Node?) {
        if(n==null) return
        val l = n.attributes.length.toInt()
        for(i in 0..(l-1)) {
            val na = n.attributes.item(i) as Attr
            if(na!=null && !list.containsKey(na.name)) {
                console.log("lost att ${na.name}")
                n.attributes.removeNamedItem(na.name)
            } else if(na!=null) {
                val a = na as DOMAttribute
                val v = list[na.nodeName]
                console.log("modify att ${a.name}: ${a.value} -> $v")
                if(v!=null)
                a.value = v.value
                else n.attributes.removeNamedItem(a.name)
            }
        }

        list.values().each {
            if(n.attributes.getNamedItem(it.name)==null) {
                console.log("gained att ${it.name}: ${it.value}")
                val a = window.document.createAttribute(it.name)!! as DOMAttribute
                a.value = it.value
                n.attributes.setNamedItem(a as Attr)
            }
        }
    }
}

fun forceId(aid : String?) : String {
    if(aid==null || aid.trim().length()==0) {
        val SESSION = (window as MyWindow)!!.bosork
        if(SESSION==null) return "id"
        return SESSION.genId()
    } else return aid
}

abstract class Tag(val name : String, val aid : String?) : HtmlElement(aid), EventManager {
    protected override val listeners: MutableMap<EventTypes, MutableSet<(DOMEvent) -> Unit>> = HashMap()
    val attributes : AttributeList = AttributeList(HashMap())

    override fun doRefresh() {
        console.log("refresh Tag $name ${id()}")
        preRefreshHook()
        dirty = false
        if(node!=null) attributes.refresh(node)
        postRefreshHook()
    }


    public override fun createNode(): Node? {
        if(hidden) return null
        console.log("create Tag $name in ${parent?.id()}: ${parent?.node?.nodeName}")
        if(parent!=null && (parent?.node!=null||parent==ROOT_PARENT)) {
            node = window.document.createElement(name)
            attributes.refresh(node)
            initListeners()
            if(parent!=ROOT_PARENT) insertIntoParent()
        }
        return node
    }
    protected open fun postRefreshHook() {

    }

    protected open fun preRefreshHook() {

    }


    fun atts(init : AttributeList.() -> Unit) {
        attributes.init()
    }

    fun addClass(c : String) {
        if(attributes.contains("class")) {
            val ca = attributes.att("class")
            val prefix = ca?.value?:""
            attributes.att("class", "$prefix $c")
        } else {
            attributes.att("class", c)
        }
    }
}



abstract class FlowContainer(s :String, id : String? = null) : Tag(s, id) {

    fun text(s:String) {
        addChild(Text(s))
    }

    fun table(id : String? = null, init: Table.() -> Unit) {
        val table = Table("", id)
        addChild(table)
        table.init()
    }

    fun a(href : String, id:String?=null, init : Link.() -> Unit) {
        val a = Link(href)
        a.init()
        addChild(a)
    }

    fun div(id:String?=null, init: Div.() -> Unit) : Div {
        val d = Div(id)
        d.init()
        addChild(d)
        return d
    }

    fun span(id:String?=null, init: Span.() -> Unit) {
        val s = Span()
        addChild(s)
        s.init()
    }
    
    fun<T,C:MutableCollection<T>> select(model:SelectionModel<T,C>,conv:Converter<T>?=null, id:String?=null, init: Select<T,C>.() -> Unit) {
        val s = Select(model, conv, id)
        s.init()
        addChild(s)
    }


    fun append(t : Table) {
        addChild(t)
    }

    fun appendFlow(c : FlowContainer) {
        addChild(c)
    }

    fun svg(w:Length,h:Length,id:String?=null, init:SVG.()->Unit) : SVG {
        val svg = SVG(Extension(w,h),id)
        svg.init()
        addChild(svg)
        return svg
    }
}

class Link(val href : String) : FlowContainer("a") {
    {
        atts { att("href", href) }
    }
    fun action(cb : Callback) {
        val SESSION = (window as MyWindow)!!.bosork!!
        val aid = SESSION.actionHolder.add(cb)
        addClass("action")
        atts {
            att("data-action", "${aid}")
        }
    }

}

class Table(public var title: String, id : String? = null) : Tag("table", id) {
    private var caption : Caption? = null
    private var body : TBody? = null
    private var head : THead? = null

    fun caption(init : Caption.() -> Unit): Unit {
        var c = Caption()
        c.init()
        caption = c
        addChild(c)
    }

    fun body(init : TBody.() -> Unit): Unit {
        val b = TBody()
        b.init()
        body = b
        addChild(b)
    }

    fun head(init : THead.() -> Unit): Unit {
        val h = THead()
        h.init()
        head = h
        addChild(h)
    }


}

class TBody(id : String? = null) : Tag("tbody", id) {

    fun tr(init : TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        addChild(row)
    }
}

class THead(id : String? = null) : Tag("thead", id) {
    fun tr(init : TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        addChild(row)
    }
}


class Caption : FlowContainer("Caption") {

}

class TableRow(id : String? = null) : Tag("tr", id) {
    fun td(init : TableCell.() -> Unit) {
        val c = TableCell()
        c.init()
        addChild(c)
    }
}


class Div(id : String? = null) : FlowContainer("div", id)
class Span(id : String? = null) : FlowContainer("span", id)
class Label(id : String? = null) : FlowContainer("label", id) {
    fun labels(ref:String) {
        attributes.att("for", ref)
    }
}

class TableCell(id : String? = null) : FlowContainer("td", id)

trait Converter<T> {
    fun convert2string(t:T) : String
    fun convert2target(s:String):T
}

class Select<T,C:MutableCollection<T>>(val model:SelectionModel<T,C>, val converter:Converter<T>?=null, id : String? = null) : Tag("select", id) {
    var listener : Callback? = null

    {
        if(model.multi) attributes.att("multiple", "true")
        else attributes.remove("multiple")
        console.log("---select init called---")
        val obs = object : AbstractObserver<T>() {
            override fun added(t: T) {
                console.log("adding option: ${t.toString()}")
                if(find(t)==null) {
                    addOption(t)
                    console.log("added option: ${t.toString()}")
                }
            }
            override fun loaded(t: T) {
                val o = find(t)
                if(o!=null) {
                    if(!o.selected()) {
                        console.log("selecting $o")
                        o.selected(true)
                        dirty = true
                    }
                }
            }
            override fun unloaded(t: T) {
                val o = find(t)
                if(o!=null) {
                    if(o.selected()) {
                        console.log("deselecting $o")
                        o.selected(false)
                        dirty = true
                    }
                }
            }
            override fun removed(t: T) {
                val o = find(t)
                if(o!=null) {
                    remove(o)
                    dirty = true
                }
            }
            override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
                val o = find(t)
                if(o != null) {
                    dirty = true
                }
            }
        }
        console.log("adding observer to $model")
        model.items.each {(t:T) ->
            addOption(t)
        }

        model.addObserver(obs)
        change {
            onSelect(it)
        }
    }

    fun addOption(t:T) {
        val cnv = converter
        option(t){label(if(cnv==null) value.toString() else cnv.convert2string(value))}
    }

    fun find(t:T) : Option<T>? {
        var found : Option<T>? = null
        each {
            if(it is Option<*> && t==it.value) {
                found = it as Option<T>
            }
        }
        return found
    }

    fun option(t:T, id:String?=null, init : Option<T>.() -> Unit) {
        console.log("create option: $t")
        val o : Option<T> = Option<T>(t, id)
        o.init()
        addChild(o)
    }

    private fun onSelect(event: DOMEvent) {
        each {
            val o = it as Option<T>
            val n = o.node
            if(n!=null) {
                val on = n as HTMLOptionElement
                if(on.selected!=o.selected()) {
                    if(on.selected) {
                        model.select(o.value)
                    } else {
                        model.deselect(o.value)
                    }
                }
            }
        }
    }


}

class Option<T>(val value:T, id : String? = null) : Tag("option", id) {
    var text : Text? = null
    fun disabled(fl : Boolean) {
        attributes.att("disabled", "${fl}")
    }
    fun selected(fl : Boolean) {
        if(fl)
        attributes.att("selected", "selected")
        else attributes.remove("selected")
    }

    fun selected() : Boolean {
        return attributes.contains("selected")
    }

    fun label(l : String) {
        attributes.att("label", l)
    }
    fun value(l : String) {
        attributes.att("value", l)
    }

}

enum class InputTypes {
    number text date datetime button checkbox
}

abstract class NumberConverter<T:Number> : Converter<Number> {
    override fun convert2string(t: Number): String {
        return "$t"
    }
    fun crtNumber(s:String) :Double{
        val mw = window as MyWindow
        return mw.parseFloat(s)
    }
}

open class IntConverter : NumberConverter<Int>() {
    override fun convert2target(s: String): Number {
        return crtNumber(s).toInt()
    }
}

open class DoubleConverter : NumberConverter<Double>() {
    override fun convert2target(s: String): Number {
        return crtNumber(s).toDouble()
    }
}

abstract class Input<T>(kind:InputTypes,val model:Model<T>,val conv:Converter<T>, id:String?=null) : Tag("input", id),EventManager {
    {
        attributes.att("type", kind.name())
        model.addObserver(object : AbstractObserver<T>() {

            override fun added(t: T) {
                value(t)
                dirty = true
            }
            override fun removed(t: T) {
                value(null)
                dirty = true
            }
            override fun deleted(t: T) {
                removed(t)
            }
            override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
                value(nv as T)
                dirty = true
            }
        })

        change {
            model.t = value()
        }
    }

    private fun value(v:T?) {
        if(v!=null)
        attributes.att("value", conv.convert2string(v))
        else attributes.att("value", "")
    }

    private fun value() : T? {
        val vs = attributes.att("value")
        if(vs!=null)  return conv.convert2target(vs.value)
        return null
    }
}

class InputNumber<T:Number>(model:Model<T>, conv:Converter<T>, id:String?=null) : Input<T>(InputTypes.number, model, conv, id)

trait EventListener {
    fun handleEvent(e:DOMEvent) : Any?
}

enum class EventTypes {
    mouseenter mouseleave click change mouseover mouseout mousemove
}

trait EventManager {
    protected val listeners : MutableMap<EventTypes,MutableSet<(e:DOMEvent)->Unit>>
    protected val node : Node?

    fun initListeners() {
        if(node!=null) {
            val et = node as EventTarget
            listeners.keySet().each {
                kind ->
                listeners[kind]?.each {
                    console.log("$kind add listener")
                    et.addEventListener(kind.name(), it, false)
                }
            }
        }
    }

    fun removeListener(kind:EventTypes, l: (e:DOMEvent)->Unit) {
        if(listeners[kind]==null) {
            return
        }
        val done : Boolean = listeners[kind]?.remove(l)?:false
        console.log("removing listener $l result: $done")
        if(node!=null) {
            val et = node as EventTarget
            et.removeEventListener(kind.name(), l)
        }
    }

    fun getListeners(kind:EventTypes) : MutableSet<(e:DOMEvent)->Unit>{
        if(listeners[kind]==null) {
            listeners.put(kind,HashSet())
        }

        return listeners[kind]!!
    }

    fun mouseenter(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.mouseenter).add(cb)
    }
    fun mouseleave(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.mouseleave).add(cb)
    }
    fun click(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.click).add(cb)
    }
    fun change(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.change).add(cb)
    }
    fun mouseover(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.mouseover).add(cb)
    }
    fun mouseout(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.mouseout).add(cb)
    }

    fun mousemove(cb:(e:DOMEvent)->Unit) {
        getListeners(EventTypes.mousemove).add(cb)
    }

}