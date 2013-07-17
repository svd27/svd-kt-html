package ch.passenger.kotlin.html.js.html

import java.util.ArrayList
import java.util.StringBuilder
import java.util.HashMap
import js.dom.html.Event
import js.jquery.jq
import ch.passenger.kotlin.html.js.Session
import js.debug.console
import js.dom.html.window

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 08.07.13
 * Time: 19:14
 * To change this template use File | Settings | File Templates.
 */

fun<T> List<T>.each(it: (T) -> Unit) :Unit {
    for(e in this)
        it(e)
}

open class Attribute(val name:String, val value:String) {
    fun render() : String {
        return "${name} = \"${value}\""
    }
}

abstract class HtmlElement(aid : String?) {
    private val children : MutableList<HtmlElement> = ArrayList<HtmlElement>()
    public val tid : String = forceId(aid)

    open fun render(): String {
        return writeChildren()
    }

    fun writeChildren() : String {
        val sb  = StringBuilder()
        //TODO: null check not needed
        children.each { if(it!=null) sb.append(it.render()) }
        return sb.toString()
    }

    fun addChild(e : HtmlElement) {
        console.log("adding: ", e.render())
        children.add(e)
    }
}

class Text(val content : String) : HtmlElement(null) {
    override fun render(): String {
        return content
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

    fun containes(name : String) : Boolean {
        return list.containsKey(name)
    }
}

fun forceId(aid : String?) : String {
    if(aid==null) {
        val SESSION = (window as MyWindow)!!.bosork!!
        return SESSION.genId()
    } else return aid
}

abstract class Tag(val name : String, val aid : String?) : HtmlElement(aid) {
    val attributes : AttributeList = AttributeList(HashMap())

    abstract fun writeContent() : String

    override fun render(): String {
        attributes.att("id", tid)
        return "<${name} ${writeAtts()}>" + writeContent() + "</${name}>"
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
        if(attributes.containes("class")) {
            val ca = attributes.att("class")
            attributes.att("class", ca?.value +" " +c)
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

    fun div(id:String?=null, init: Div.() -> Unit) {
        val d = Div()
        d.init()
        addChild(d)
    }

    fun span(init: Span.() -> Unit) {
        val s = Span()
        addChild(s)
        s.init()
    }
    
    fun select(id:String?=null, init: Select.() -> Unit) {
        val s = Select()
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
}


class Div(id : String? = null) : FlowContainer("div", id) {
}

class Span(id : String? = null) : FlowContainer("span", id) {
}

class TableCell(id : String? = null) : FlowContainer("td", id)

class Select(id : String? = null) : Tag("select", id) {
    override fun writeContent(): String {
        return writeChildren()
    }
    
    fun<T> option(t:T, id:String, init : Option<T>.() -> Unit) {
        val o = Option(t, id)
        o.init()
        addChild(o)
    }

    fun change(cb : Callback) {
        val SESSION = (window as MyWindow)!!.bosork!!
        val aid = SESSION.actionHolder.add(cb)
        addClass("action")
        atts {
            att("data-action", "${aid}")
        }
    }
}

class Option<T>(t:T, id : String? = null) : Tag("option", id) {
    var text : Text? = null
    fun disabled(fl : Boolean) {
        attributes.att("disabled", "${fl}")
    }
    fun selected(fl : Boolean) {
        attributes.att("selected", "${fl}")
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
}