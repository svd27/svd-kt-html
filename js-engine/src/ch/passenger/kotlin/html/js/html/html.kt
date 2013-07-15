package ch.passenger.kotlin.html.js.html

import java.util.ArrayList
import java.util.StringBuilder
import java.util.HashMap
import js.dom.html.Event
import js.jquery.jq
import ch.passenger.kotlin.html.js.Session
import ch.passenger.kotlin.html.js.SESSION

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

abstract class HtmlElement() {
    val children : MutableList<HtmlElement> = ArrayList<HtmlElement>()

    open fun render(): String {
        return writeChildren()
    }

    fun writeChildren() : String {
        val sb  = StringBuilder()
        children.each { sb.append(it.toString()) }
        return sb.toString()
    }


}

class Text(val content : String) : HtmlElement() {
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
    if(aid==null) return SESSION.genId()
    else return aid
}

abstract class Tag(val name : String, val aid : String?) : HtmlElement() {
    val attributes : AttributeList = AttributeList(HashMap())
    public var tid : String = forceId(aid)


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

abstract class FlowContent(s :String, id : String? = null) : Tag(s, id) {
    fun text(s:String) {
        children.add(Text(s))
    }

    fun table(id : String, init: Table.() -> Unit) {
        val table = Table(id)
        children.add(table)
        table.init()
    }

    fun table(init: Table.() -> Unit) {
        val table = Table()
        children.add(table)
        table.init()
    }

    fun a(href : String, init : Link.() -> Unit) {
        val a = Link(href)
        a.init()
        children.add(a)
    }

    override final fun writeContent(): String {
        return writeChildren()
    }
}

class Link(val href : String) : FlowContent("a") {
    fun action(cb : Callback) {
        val aid = SESSION.actionHolder.add(cb)
        addClass("action")
        atts {
            att("data-action", "${aid}")
        }
    }
}

class Table(id : String? = null) : Tag("table", id) {
    var caption : Caption? = null
    var body : TBody? = null
    var head : THead? = null

    override fun writeContent(): String {
        val sb : StringBuilder = StringBuilder()
        if(caption!=null) sb.append(caption.toString())
        if(head!=null) sb.append(head.toString())
        if(body!=null) sb.append(body.toString())
        return sb.toString()
    }

    fun caption(init : Caption.() -> Unit): Unit {
        var c = Caption()
        c.init()
        caption = c
    }

    fun body(init : TBody.() -> Unit): Unit {
        val b = TBody()
        b.init()
        body = b
    }

    fun head(init : THead.() -> Unit): Unit {
        val h = THead()
        h.init()
        head = h
    }

}

class TBody(id : String? = null) : Tag("tbody", id) {

    override fun writeContent(): String {
        return writeChildren()
    }

    fun tr(init : TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        children.add(row)
    }
}

class THead(id : String? = null) : Tag("thead", id) {

    override fun writeContent(): String {
        return writeChildren()
    }

    fun tr(init : TableRow.() -> Unit): Unit {
        val row = TableRow()
        row.init()
        children.add(row)
    }
}


class Caption : FlowContent("Caption") {

}

class TableRow(id : String? = null) : Tag("tr", id) {
    fun td(init : TableCell.() -> Unit) {
        val c = TableCell()
        c.init()
        children.add(c)
    }

    override fun writeContent(): String {
        return writeChildren()
    }
}

class Div(id : String? = null) : FlowContent("div", id) {
}

class Span(id : String? = null) : FlowContent("span", id) {
}

class TableCell(id : String? = null) : FlowContent("td", id) {


}