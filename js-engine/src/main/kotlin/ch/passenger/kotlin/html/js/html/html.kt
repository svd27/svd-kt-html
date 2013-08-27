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
import ch.passenger.kotlin.html.js.html.util.Converter
import ch.passenger.kotlin.html.js.html.util.BooleanConverter
import js.dom.html.HTMLInputElement
import ch.passenger.kotlin.html.js.css.*
import ch.passenger.kotlin.html.js.model.Observable
import ch.passenger.kotlin.html.js.model.AbstractSelectionModel
import ch.passenger.kotlin.html.js.logger.Logger

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 08.07.13
 * Time: 19:14
 * To change this template use File | Settings | File Templates.
 */

fun session(): Session {
    val mw = window as MyWindow
    return mw.bosork!!
}

open class Attribute(val name: String, val value: String) {
    public fun render(): String {
        return "${name} = \"${value}\""
    }
}

public val ROOT_PARENT: HtmlElement = Text("")

abstract class HtmlElement(aid: String?) : Dirty {
    var parent: HtmlElement? = null
    var node: Node? = null
    var hidden: Boolean = false
        set(v) {
            if(v != hidden) {
                $hidden = v
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
                //console.log("$desc ${this.id()} wants a refresh")
                val SESSION = (window as MyWindow)!!.bosork!!
                SESSION.refresh(this)
            }
        }
    private val _children: MutableList<HtmlElement> = ArrayList<HtmlElement>()
    protected val children: List<HtmlElement>
        get() = _children

    public val tid: String = forceId(aid)
    public abstract fun createNode(): Node?
    public open fun postChildrenRendered() {}

    public fun each(cb: (el: HtmlElement) -> Unit) {
        children.each { cb(it) }
    }

    public fun id(): String = tid

    abstract fun doRefresh()
    fun refresh() {
        dirty = false
        doRefresh()
    }

    public open fun addChild(e: HtmlElement) {
        doAddChild(e)
    }

    protected fun doAddChild(e:HtmlElement) {
        e.parent = this
        //console.log("adding: ", e)
        _children.add(e)
    }



    fun find(id: String): HtmlElement? {
        var result: HtmlElement? = null
        if(id == id()) return this
        each {
            result = it.find(id)
        }

        return result
    }


    public fun precedingSibling(e: HtmlElement): HtmlElement? {
        val idx = indexOf(e)
        if(idx == 0) return null
        if(idx > 0) return children[idx - 1]
        var res: HtmlElement? = null
        each {
            val s = it.precedingSibling(e)
            if(s != null) res = s
        }

        return res
    }

    public fun nextSibling(e: HtmlElement): HtmlElement? {
        val idx = indexOf(e)
        if(idx == 0) return if(children.size() > 1) children[1] else null
        if(idx > 0) return if(children.size() > idx + 1) children[idx + 1] else null
        var res: HtmlElement? = null
        each {
            val s = it.nextSibling(e)
            if(s != null) res = s
        }

        return res
    }

    fun indexOf(e: HtmlElement): Int {
        var idx = -1
        children.eachIdx {
            (i, c) ->
            if(c.id() == e.id()) idx = i
        }
        return idx
    }

    open fun detach() {
        if (node != null) {
            if(parent!=null) {
                val p = parent!!
                if(p.node!=null) {
                    val n = p.node!!
                    console.log("removing ", n)
                    n!!.removeChild(node!!)
                }
            }
            node = null
            each { it.detach() }
        }
    }

    public fun clear(refresh: Boolean = false) {
        val cl = ArrayList<HtmlElement>()
        cl.addAll(children)
        cl.each { remove(it) }
        if(refresh) dirty = true
    }

    protected fun remove(c: HtmlElement) {
        val idx = indexOf(c)
        if(idx >= 0) {
            c.clear()
            c.detach()

            //should have no more references to DOM after detaching all
            _children.remove(idx)

        }
    }

    fun insertIntoParent() {
        var idx = -1

        parent?.children?.eachIdx {
            i, me ->
            if(me.id() == id())
                idx = i
        }
        if(idx < 0) throw IllegalStateException()

        var sib: HtmlElement? = null
        var preceed = true
        if(idx == 0) {
            preceed = true
            if(parent?.children?.size()?:0 > 1)
                sib = parent?.children?.get(1)
        } else {
            preceed = false
            sib = parent?.children?.get(idx - 1)
        }


        if(sib != null && sib?.node != null) {
            if(preceed) {
                parent?.node?.insertBefore(node!!, sib?.node!!)
            } else {
                if(sib?.node?.nextSibling != null) {
                    parent?.node?.insertBefore(node!!, sib?.node!!.nextSibling!!)
                } else {
                    parent?.node?.appendChild(node!!)
                }
            }
        } else {
            parent?.node?.appendChild(node!!)
        }
    }
}

class Text(initial: String) : HtmlElement(null) {
    var content: String = initial
        set(s) {
            if(content != s) {
                $content = s; dirty = true
            }
        }

    public override fun createNode(): Node? {
        if(hidden) return null
        if(parent != null && (parent?.node != null || parent == ROOT_PARENT)) {
            node = window.document.createTextNode(content)!!
            if(parent != ROOT_PARENT) insertIntoParent()
        }
        return node
    }


    override fun doRefresh() {
        if(parent!=null) {
           val p = parent!!
           if(p.node!=null) p.node!!.textContent = content
        }
    }


    override fun detach() {
        if(parent!=null) {
            val p = parent!!
            if(p.node!=null) p.node!!.textContent = ""
        }
    }
}



class AttributeList(private val list: MutableMap<String, Attribute>) {
    fun att(name: String, value: String) {
        list.put(name, Attribute(name, value))
    }

    fun values(): MutableCollection<Attribute> {
        return list.values()
    }

    fun att(name: String): Attribute? {
        return list.get(name)
    }

    fun contains(name: String): Boolean {
        return list.containsKey(name)
    }

    fun remove(name: String) {
        list.remove(name)
    }

    fun refresh(n: Node?) {
        if(n == null) return
        val l = n.attributes.length.toInt()
        for(i in 0..(l - 1)) {
            val na = n.attributes.item(i) as Attr
            if(na != null && !list.containsKey(na.name)) {
                //console.log("lost att ${na.name}")
                n.attributes.removeNamedItem(na.name)
            } else if(na != null) {
                val a = na as DOMAttribute
                val v = list[na.nodeName]
                //console.log("modify att ${a.name}: ${a.value} -> ", v?.value)
                if(v != null)
                    a.value = v.value
                else n.attributes.removeNamedItem(a.name)
            }
        }

        list.values().each {
            if(n.attributes.getNamedItem(it.name) == null) {
                //console.log("gained att ${it.name}: ${it.value}")
                val a = window.document.createAttribute(it.name)!! as DOMAttribute
                a.value = it.value
                n.attributes.setNamedItem(a as Attr)
            }
        }
    }
}

fun forceId(aid: String?): String {
    if(aid == null || aid.trim().length() == 0) {
        val SESSION = (window as MyWindow)!!.bosork
        if(SESSION == null) return "id"
        return SESSION.genId()
    } else return aid
}

abstract class Tag(val name: String, val aid: String?) : HtmlElement(aid), EventManager {
    protected override val listeners: MutableMap<EventTypes, MutableSet<(DOMEvent) -> Unit>> = HashMap()
    val attributes: AttributeList = AttributeList(HashMap())
    val styles : MutableMap<String,CSSProperty<*>> = HashMap()

    override fun doRefresh() {
        //console.log("refresh Tag $name ${id()}")
        preRefreshHook()
        dirty = false
        if(node != null) {
            if (styles.size()>0) {
                val sb = StringBuilder()
                styles.values().each {
                    sb.append(it.write())
                    sb.append(" ")
                }
                if(sb.toString().length()>0)
                  attributes.att("style", sb.toString())
            }
            attributes.refresh(node)
        }
        postRefreshHook()
    }


    public override fun createNode(): Node? {
        if(hidden) return null
        console.log("create Tag $name in ${parent?.id()}: ${parent?.node?.nodeName}")
        if(parent != null && (parent?.node != null || parent == ROOT_PARENT)) {
            node = window.document.createElement(name)
            if(id().trim().length()>0) {
                attributes.att("id", id())
            }
            attributes.refresh(node)
            initListeners()
            if(parent != ROOT_PARENT) insertIntoParent()
        }
        return node
    }
    protected open fun postRefreshHook() {

    }

    protected open fun preRefreshHook() {

    }


    fun atts(init: AttributeList.() -> Unit) {
        attributes.init()
    }

    private val classes : MutableSet<String> = HashSet()

    fun addClass(c: String) {
        classes.add(c)
        val sb = StringBuilder()
        classes.each {
            sb.append(it)
                sb.append(' ')
        }
        if(sb.toString().length()>0)
        attributes.att("class", sb.toString())
    }

    fun removeClass(c: String) {
        classes.remove(c)
        val sb = StringBuilder()
        classes.each { sb.append(it)
                sb.append(' ') }
        if(sb.toString().length()>0)
            attributes.att("class", sb.toString())
    }


    fun addStyle(s: String, vararg v:String) :CSSStringProperty {
        styles.put(s, CSSStringProperty(s, v))
        val res = styles.get(s)
        return res as CSSStringProperty
    }

    fun addStyle(s: String, vararg v:Length):CSSLengthProperty {
        styles.put(s, CSSLengthProperty(s, v))
        val res = styles.get(s)
        return res as CSSLengthProperty
    }

    fun addStyle(v:CSSProperty<*>): CSSProperty<*>? {
        styles.put(v.name, v)
        return styles.get(v)
    }

    fun styles() : Collection<CSSProperty<*>> = styles.values()
}



abstract class FlowContainer(s: String, id: String? = null) : Tag(s, id) {
    fun HtmlElement.plus() = this@FlowContainer.addChild(this)

    fun text(s: String) {
        addChild(Text(s))
    }

    fun table(id: String? = null, init: Table.() -> Unit) {
        val table = Table("", id)
        addChild(table)
        table.init()
    }

    fun a(text:String="", href: String="#", id: String? = null, init: Link.() -> Unit) : Link {
        val a = Link(href)
        a.init()
        addChild(a)
        return a
    }

    fun div(id: String? = null, init: Div.() -> Unit): Div {
        val d = Div(id)
        d.init()
        addChild(d)
        return d
    }

    fun span(id: String? = null, init: Span.() -> Unit): Span {
        val s = Span()
        addChild(s)
        s.init()
        return s
    }

    fun<T> select(model: SelectionModel<T>, conv: Converter<T>? = null, id: String? = null, init: Select<T>.() -> Unit):
            Select<T> {
        val s = Select(model, conv, id)
        s.init()
        addChild(s)
        return s
    }

    fun label(lfor: String? = null, id: String? = null, init: Label.()->Unit): Label {
        val l = Label(id)
        if(lfor != null) l.labels(lfor)
        l.init()
        addChild(l)
        return l
    }

    fun checkbox(model: Model<Boolean>, id: String? = null, init: CheckBox.()->Unit): CheckBox {
        val cb = CheckBox(model, id)
        cb.init()
        addChild(cb)
        return cb
    }

    fun append(t: Table) {
        addChild(t)
    }

    fun appendFlow(c: FlowContainer) {
        addChild(c)
    }

    fun svg(w: Length, h: Length, id: String? = null, init: SVG.()->Unit): SVG {
        val svg = SVG(Extension(w, h), id)
        svg.init()
        addChild(svg)
        return svg
    }

    fun border(id:String?=null,init:BorderLayout.()->Unit) : BorderLayout {
        val b = BorderLayout(id, init)
        addChild(b)
        return b
    }

    fun inputText(initial:String, id:String?=null, init:InputText.()->Unit) : InputText {
        val m = object : Model<String> {
            protected override var _value: String? = initial
            protected override val observers: MutableSet<Observer<String>> = HashSet()
        }
        val inp = InputText(m, StringConverter(), id)
        inp.init()
        return inp
    }

    fun inputText(m:Model<String>, id:String?=null, init:InputText.()->Unit) : InputText {
        val inp = InputText(m, StringConverter(), id)
        inp.init()
        return inp
    }


    fun textArea(m:Model<String> = DefaultStringModel(), id:String?=null, init:TextArea.()->Unit) : TextArea {
        val ta = TextArea(m, id)
        ta.init()
        addChild(ta)
        return ta
    }

}

class Link(val text:String="", val href: String="#") : FlowContainer("a") {
    {
        attributes.att("href", href)
        if(text.trim().length()>0)
        text(text)
    }
}

class Table(public var title: String, id: String? = null) : Tag("table", id) {
    private var caption: Caption? = null
    private var body: TBody? = null
    private var head: THead? = null

    fun caption(init: Caption.() -> Unit): Unit {
        var c = Caption()
        c.init()
        caption = c
        addChild(c)
    }

    fun body(init: TBody.() -> Unit): Unit {
        val b = TBody()
        b.init()
        body = b
        addChild(b)
    }

    fun head(init: THead.() -> Unit): Unit {
        val h = THead()
        h.init()
        head = h
        addChild(h)
    }


}

class TBody(id: String? = null) : Tag("tbody", id) {

    fun tr(init: TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        addChild(row)
    }
}

class THead(id: String? = null) : Tag("thead", id) {
    fun tr(init: TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        addChild(row)
    }
}


class Caption : FlowContainer("Caption") {

}

class TableRow(id: String? = null) : Tag("tr", id) {
    fun td(init: TableCell.() -> Unit) {
        val c = TableCell()
        c.init()
        addChild(c)
    }
}


class Div(id: String? = null) : FlowContainer("div", id)
class Span(id: String? = null) : FlowContainer("span", id)
class Label(id: String? = null) : FlowContainer("label", id) {
    fun labels(ref: String) {
        attributes.att("for", ref)
    }
}

class TableCell(id: String? = null) : FlowContainer("td", id)



class Select<T>(val model: SelectionModel<T>, val converter: Converter<T>? = null, id: String? = null) : Tag("select", id) {
    var listener: Callback? = null
    private val log = Logger.logger("select");

    {
        if(model.multi) attributes.att("multiple", "true")
        else attributes.remove("multiple")
        console.log("---select init called---")
        val obs = object : AbstractObserver<T>() {
            override fun added(t: T) {
                log.debug("adding option: ", t)
                console.log("adding option: ${t.toString()}")
                if(find(t) == null) {
                    addOption(t).dirty = true
                    console.log("added option: ${t.toString()}")
                }
            }
            override fun loaded(t: T) {
                if (model.multi) {
                    val o = find(t)
                    if(o != null) {
                        if(!o.selected()) {
                            console.log("selecting $o")
                            o.selected(true)
                            o.dirty = true
                        }
                    }
                } else {
                    var so : Option<*>? =null
                    each {
                        if(it is Option<*>) {
                            val o = it as Option<T>
                            if(o.value==t && !o.selected()) {
                                o.selected(true)
                                o.dirty = true
                                so = o
                            } else if(o.selected()) {
                                o.selected(false)
                                o.dirty = true
                            }
                        }
                    }
                    so?.selected(true)
                    so?.dirty = true
                }
                dirty = true
            }
            override fun unloaded(t: T) {
                val o = find(t)
                if(o != null) {
                    if(o.selected()) {
                        console.log("deselecting $o")
                        o.selected(false)
                        dirty = true
                    }
                }


            }
            override fun removed(t: T) {
                val o = find(t)
                if(o != null) {
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
        model.items.each {(t: T) ->
            addOption(t)
        }

        model.addObserver(obs)
        change {
            onSelect(it)
        }
    }


    public override fun postChildrenRendered() {
        each {
            val o = it as Option<T>
            if(o.node!=null) {
                val sn = o.node as HTMLOptionElement
                if(o.selected() != sn.selected) {
                    o.refresh()
                }
            } else {
                console.log(o, " has no node!")
            }
        }
    }
    fun select(t: T) {
        model.select(t)
    }

    fun selected(): T? {
        var sel: T? = null
        each {
            if(it is Option<*> && it.selected()) sel = it.value as T?
        }
        return  sel
    }

    fun addOption(t: T) : Option<T> {
        val cnv = converter
        return option(t) { label(if(cnv == null) value.toString() else cnv.convert2string(value)) }
    }

    fun find(t: T): Option<T>? {
        var found: Option<T>? = null
        each {
            if(it is Option<*> && t == it.value) {
                found = it as Option<T>
            }
        }
        return found
    }

    fun option(t: T, id: String? = null, init: Option<T>.() -> Unit) : Option<T> {
        console.log("create option: $t")
        val o: Option<T> = Option<T>(t, id)
        o.init()
        addChild(o)
        return o
    }

    private fun onSelect(event: DOMEvent) {
        console.log("onSelect: ", event)
        val sels = ArrayList<Option<T>>()
        each {
            val o = it as Option<T>
            val n = o.node
            if(n != null) {
                val on = n as HTMLOptionElement
                if(on.selected) {
                    sels.add(o)
                } else {
                    model.deselect(o.value)
                }
                if(on.selected != o.selected()) {
                    console.log(o.value, " node!=option node: ", on.selected, " option ", o.selected())
                    o.selected(o.selected())
                    o.dirty = true
                }
            }
        }
        sels.each {
            console.log("telling model to select ", it.value)
            model.select(it.value)
        }
    }
}

class Option<T>(val value: T, id: String? = null) : Tag("option", id) {
    var text: Text = Text("")
    fun disabled(fl: Boolean) {
        attributes.att("disabled", "${fl}")
    }
    fun selected(fl: Boolean) {
        if(node!=null) {
            val on = node as HTMLOptionElement
            if(on.selected!=fl) on.selected = fl
        }
        if(fl) attributes.att("selected", "selected")
        else attributes.remove("selected")
    }

    fun selected(): Boolean {
        if(node!=null) {
            val on = node as HTMLOptionElement
            return on.selected
        }
        return attributes.att("selected")!=null
    }

    fun label(l: String) {
        attributes.att("label", l)
        text.detach()
        text = Text(l)
        addChild(text)
    }
    fun value(l: String) {
        attributes.att("value", l)
    }
}

enum class InputTypes {
    number text date datetime button checkbox
}


abstract class Input<T>(kind: InputTypes, val model: Model<T>, val conv: Converter<T>, id: String? = null) : Tag("input", id), EventManager {
    protected val log: Logger = Logger.logger("html.input");
    {
        attributes.att("type", kind.name())
        model.addObserver(object : AbstractObserver<T>() {

            override fun added(t: T) {
                _value(t)
                dirty = true
            }
            override fun removed(t: T) {
                _value(null)
                dirty = true
            }
            override fun deleted(t: T) {
                removed(t)
            }
            override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
                _value(nv as T?)
                dirty = true
            }
        })

        change {
            log.debug("received new value: ", model.value, " -> ", _value())
            model.value = _value()
        }
        if(model.value!=null)
        attributes.att("value", conv.convert2string(model.value!!))
    }

    public fun value() : T? = model.value
    public fun value(t:T?) : Unit = model.value = t

    protected open fun _value(v: T?) {
        val n = node as HTMLInputElement
        if(n!=null)
            n.value = if(v!=null) conv.convert2string(v) else ""
    }

    protected open fun _value(): T? {
        val n = node as HTMLInputElement
        if(n!=null) return conv.convert2target(n.value)
        return null
    }
}

class StringConverter() : Converter<String> {

    override fun convert2string(t: String): String {
        return t
    }
    override fun convert2target(s: String): String {
        return s
    }
}

class DefaultStringModel : Model<String> {
    protected override val observers: MutableSet<Observer<String>> = HashSet()
    protected override var _value: String? = null
}

class InputText(m:Model<String> = DefaultStringModel(), conv:Converter<String> = StringConverter(), id:String?=null) :
Input<String>(InputTypes.text, m, conv, id)

class TextArea(val model:Model<String> = DefaultStringModel(), id:String?=null) : Tag("textarea", id){
    var rows : Int = 5
    set(v) {
        $rows = v
        attributes.att("rows", "$v")
    }

    var cols : Int = 5
        set(v) {
            $cols = v
            attributes.att("cols", "$v")
        }
    var readonly : Boolean = false
        set(v) {
            $readonly = v
            if(v)
            attributes.att("readOnly", "$v")
            else attributes.remove("readOnly")
        }


    public fun value() : String? = model.value
    public fun value(t:String?) : Unit = model.value = t
}

class CheckBox(model: Model<Boolean>, id: String? = null) : Input<Boolean>(InputTypes.checkbox, model, BooleanConverter(), id) {

    protected override fun _value(): Boolean? {
        if(node == null) null
        val inp = node as HTMLInputElement
        return inp.checked
    }
    protected override fun _value(v: Boolean?) {
        if(node == null) return
        val inp = node as HTMLInputElement
        if(v != null)
            inp.checked = v
        else inp.checked = false
    }

    {
        model.addObserver(object:AbstractObserver<Boolean>() {
            override fun updated(t: Boolean, prop: String, old: Any?, nv: Any?) {
                if(t==value()) return
                value(t)
                dirty = true
            }
        })
    }
}

class InputNumber<T : Number>(model: Model<T>, conv: Converter<T>, id: String? = null) : Input<T>(InputTypes.number, model, conv, id)

trait EventListener {
    fun handleEvent(e: DOMEvent): Any?
}

enum class EventTypes {
    mouseenter mouseleave click change mouseover mouseout mousemove keypress keydown keyup end begin
}

trait EventManager {
    protected val listeners: MutableMap<EventTypes, MutableSet<(e: DOMEvent)->Unit>>
    protected val node: Node?

    fun initListeners() {
        if(node != null) {
            val et = node as EventTarget
            listeners.keySet().each {
                kind ->
                listeners[kind]?.each {
                    console.log("$kind add listener to ", node)
                    et.addEventListener(kind.name(), it, false)
                }
            }
        }
    }

    fun removeListener(kind: EventTypes, l: (e: DOMEvent)->Unit) {
        if(listeners[kind] == null) {
            return
        }
        val done: Boolean = listeners[kind]?.remove(l)?:false
        console.log("removing listener $l result: $done")
        if(node != null) {
            val et = node as EventTarget
            et.removeEventListener(kind.name(), l)
        }
    }

    fun getListeners(kind: EventTypes): MutableSet<(e: DOMEvent)->Unit> {
        if(listeners[kind] == null) {
            listeners.put(kind, HashSet())
        }

        return listeners[kind]!!
    }

    fun add(kind:EventTypes,cb: (e: DOMEvent)->Unit) = getListeners(kind).add(cb)
    fun mouseenter(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.mouseenter).add(cb)
    }
    fun mouseleave(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.mouseleave).add(cb)
    }
    fun click(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.click).add(cb)
    }
    fun change(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.change).add(cb)
    }
    fun mouseover(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.mouseover).add(cb)
    }
    fun mouseout(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.mouseout).add(cb)
    }

    fun mousemove(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.mousemove).add(cb)
    }

    fun keypress(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.keypress).add(cb)
    }

    fun keydown(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.keydown).add(cb)
    }

    fun keyupx(cb: (e: DOMEvent)->Unit) {
        getListeners(EventTypes.keyup).add(cb)
    }


}