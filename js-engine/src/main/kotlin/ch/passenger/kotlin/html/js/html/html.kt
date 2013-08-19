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

abstract class HtmlElement(aid : String?) : Dirty {
    public override var dirty: Boolean = false
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
    protected val children : MutableList<HtmlElement> = ArrayList<HtmlElement>()
    public val tid : String = forceId(aid)

    public fun each(cb: (el:HtmlElement) -> Unit) {
        children.each { cb(it) }
    }

    public fun id() : String = tid

    public open fun render(): String {
        dirty = false
        return writeChildren()
    }

    abstract fun refresh(n:Node)

    fun writeChildren() : String {
        val sb  = StringBuilder()
        children.each { sb.append(it.render()) }
        return sb.toString()
    }

    fun addChild(e : HtmlElement) {
        console.log("adding: ", e.render())
        children.add(e)
    }

    fun find(id:String) : HtmlElement? {
        var result : HtmlElement? = null
        if(id==id()) return this
        each {
            result = it.find(id)
        }

        return result
    }

    public fun parent(e:HtmlElement) : HtmlElement? {
        var res : HtmlElement? = null
        each { if(it.id()==e.id()) res = this }
        if(res==null) {
            each {
                var parent : HtmlElement? = null
                parent = it.parent(e)
                if(parent!=null) res = parent
            }
        }
        return res
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
        if(idx>0) return if(children.size()>idx+1) children[idx+1] else children[idx-1]
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
}

class Text(val content : String) : HtmlElement(null) {
    public override fun render(): String {
        dirty = false
        return content
    }


    override fun refresh(n: Node) {
        n.textContent = content
    }
}


native trait DOMAttribute {
    public native val name: String
    public native var specified: Boolean
    public native var value: String
    public native var ownerElement: Element
    public native var schemaTypeInfo: TypeInfo
    public native var isId: Boolean
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

    fun refresh(n:Node) {
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

abstract class Tag(val name : String, val aid : String?) : HtmlElement(aid) {
    val attributes : AttributeList = AttributeList(HashMap())

    abstract fun writeContent() : String
    abstract fun preRender()

    public override fun render(): String {
        console.log("render tag $name:${id()}")
        preRender()
        dirty = false
        attributes.att("id", tid)
        return "<${name} ${writeAtts()}>" + writeContent() + "</${name}>"
    }


    override final fun refresh(n: Node) {
        console.log("refresh Tag $name ${id()}")
        preRefreshHook(n)
        dirty = false
        attributes.refresh(n)
        refreshHook(n)
        each {
            session().refresh(it)
        }
    }

    protected open fun refreshHook(n:Node) {

    }

    protected open fun preRefreshHook(n:Node) {

    }

    fun writeAtts(): String {
        val sb = StringBuilder()
        for(a in attributes.values()) {
            sb.append(a.render())
            sb.append(" ")
        }

        return sb.toString()
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

    public fun clear() {
        console.log("${id()} clearing")
        children.clear()
    }
}



abstract class FlowContainer(s :String, id : String? = null) : Tag(s, id) {

    override open fun preRender() {

    }

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

    override final fun writeContent(): String {
        return writeChildren()
    }

    fun appendFlow(c : FlowContainer) {
        addChild(c)
    }

    fun svg(w:Length,h:Length,id:String?=null, init:SVG.()->Unit) : FlowContainer {
        val svg = SVG(Extension(w,h),id)
        svg.init()
        addChild(svg)
        return this
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
    var caption : Caption? = null
    var body : TBody? = null
    var head : THead? = null

    override fun writeContent(): String {
        val sb : StringBuilder = StringBuilder()
        if(caption!=null) sb.append(caption?.render())
        if(head!=null) sb.append(head?.render())
        if(body!=null) sb.append(body?.render())
        return sb.toString()
    }

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


    override fun preRender() {

    }
}

class TBody(id : String? = null) : Tag("tbody", id) {

    override fun writeContent(): String {
        return writeChildren()
    }

    fun tr(init : TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        addChild(row)
    }


    override fun preRender() {

    }
}

class THead(id : String? = null) : Tag("thead", id) {

    override fun writeContent(): String {
        return writeChildren()
    }

    fun tr(init : TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        addChild(row)
    }


    override fun preRender() {

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

    override fun writeContent(): String {
        return writeChildren()
    }


    override fun preRender() {

    }
}


class Div(id : String? = null) : FlowContainer("div", id)
class Span(id : String? = null) : FlowContainer("span", id)

class TableCell(id : String? = null) : FlowContainer("td", id)

trait Converter<T> {
    fun convert2string(t:T) : String
    fun convert2target(s:String):T
}

class Select<T,C:MutableCollection<T>>(val model:SelectionModel<T,C>, val converter:Converter<T>?=null, id : String? = null) : Tag("select", id) {
    var listener : Callback? = null
    private val options : MutableList<Option<T>> = ArrayList<Option<T>>();

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
                    options.remove(o)
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
        val SESSION = (window as MyWindow)!!.bosork!!
        val cb = object : Callback {

            override fun callback(event: DOMEvent) {
                change(event)
            }
        }
        val aid = SESSION.actionHolder.add(cb)
        addClass("change")
        atts {
            att("data-action", "${aid}")
        }
    }

    fun addOption(t:T) {
        val cnv = converter
        option(t){text(if(cnv==null) value.toString() else cnv.convert2string(value))}
    }

    fun find(t:T) : Option<T>? {
        var found : Option<T>? = null
        options.each {
            if(t==it.value) {
                found = it
            }
        }
        return found
    }



    override fun writeContent(): String {
        val sb = StringBuilder()
        options.each { sb.append(it.render()) }
        return sb.toString()
    }


    override fun refreshHook(n: Node) {
        options.each {
            session().refresh(it)
        }
    }
    fun option(t:T, id:String?=null, init : Option<T>.() -> Unit) {
        console.log("create option: $t")
        val o : Option<T> = Option<T>(t, id)
        o.init()
        options.add(o)
    }

    private fun change(event: DOMEvent) {
        val sel = jq("#${event.target.id} option")

        val hsel = window.document.getElementById(id()) as HTMLSelectElement

        console.log("hsel.options.length: ${hsel.options.length.toInt()} -> ${((hsel.options.length.toInt())-1)}")
        for(i in 0..((hsel.options.length.toInt())-1)) {
            val hopt = hsel.options.item(i) as HTMLOptionElement
            val opt = options[i]
            console.log("comparing ${hopt.value} with opt: ${opt.value.toString()}")
            if(hopt.selected==opt.selected()) continue
            if(hopt.selected) model.select(opt.value)
            else model.deselect(opt.value)
        }

        event.data = sel.value()

    }


    override fun preRender() {

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
    
    fun text(s : String) {
        text = Text(s)
    }
    
    override fun writeContent(): String {
        if(text!=null) return text?.render()!!
        return ""
    }


    override fun preRender() {

    }
}