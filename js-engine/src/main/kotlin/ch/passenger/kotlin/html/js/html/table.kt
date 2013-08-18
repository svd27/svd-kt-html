package ch.passenger.kotlin.html.js.html

import ch.passenger.kotlin.html.js.model.Identifiable
import java.util.ArrayList
import java.util.HashSet
import js.jquery.jq
import java.util.HashMap
import ch.passenger.kotlin.html.js.model.Observer

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 11.07.13
 * Time: 17:06
 * To change this template use File | Settings | File Templates.
 */

trait TableListener<T> {
    fun rowAdded(row: Int, t: T)
    fun rowRemove(row: Int, t: T)
    fun rowUpdated(row: Int, t: T)
    fun cellUpdated(row: Int, cell: Int, t: T, property: String, ov: Any?, nv: Any?)
}


abstract class TableModel<T: Identifiable>(): Observer<T>   {
    public val content: MutableList<T> = ArrayList()
    private val listeners: MutableSet<TableListener<T>> = HashSet()
    public val columns: MutableList<String> = ArrayList()
    public var title: String = "Table"


    override fun unloaded(t: T) {
        removed(t)
    }
    override fun loaded(t: T) {
        for(i in 0..content.size()) {
            if(content[i].id.equals(t.id)) {
                content[i] = t
                updateRow(i, t)
                return
            }
        }
        content.add(t)
        addRow(content.size() - 1, t)
    }

    fun updateRow(i: Int, t: T) {
        for(l in listeners) l.rowUpdated(i, t)
    }

    fun addRow(i: Int, t: T) {
        for(l in listeners) l.rowAdded(i, t)
    }

    override fun removed(t: T) {
        throw UnsupportedOperationException()
    }
    override fun deleted(t: T) {
        throw UnsupportedOperationException()
    }
    override fun updated(t: T, prop: String, old: Any?, nv: Any?) {
        throw UnsupportedOperationException()
    }

    override fun added(t: T) {
        throw UnsupportedOperationException()
    }

    abstract fun value(t: T, col: String): Any?
    abstract fun value(t: T, col: String, v: Any?)
}

class TableRenderer<T: Identifiable>(val model: TableModel<T>, val ids: String): TableListener<T> {
    var title : String = model.title
    val renderers : MutableMap<String,CellRenderer<T>> = HashMap()
    val defaultRenderer : CellRenderer<T> = object : CellRenderer<T> {}

    override fun rowAdded(row: Int, t: T) {
        throw UnsupportedOperationException()
    }
    override fun rowRemove(row: Int, t: T) {
        throw UnsupportedOperationException()
    }
    override fun rowUpdated(row: Int, t: T) {
        throw UnsupportedOperationException()
    }
    override fun cellUpdated(row: Int, cell: Int, t: T, property: String, ov: Any?, nv: Any?) {
        throw UnsupportedOperationException()
    }

    fun renderer(col:String) : CellRenderer<T> {
        if(renderers.containsKey(col)) {
            return renderers[col]!!
        }

        return defaultRenderer
    }


    fun appendTo(e: FlowContainer, m : TableModel<T> = model, tablerenderer : TableRenderer<T> = this) {
        val t : Table = Table(ids)
        t.title = title
        t.caption {
            text(t.title)
        }

        t.head {
            atts {
                att("data-table-head", "true")
            }
            tr {
                atts {
                    att("data-table-head-row", "1")
                }
                for(c in m.columns) {
                    td {
                        atts { att("data-table-head-row-column", "${c}") }
                        text(c)
                    }
                }
            }
        }

        t.body {
            for(row in 0..(m.content.size()-1))
                tr {
                    atts {
                        att("data-table-row", "${row}")
                    }
                    for(c in m.columns)
                        td {
                            atts { att("data-table-cell", "r${row}c${c}") }
                            val v = m.value(m.content[row], c)

                            val cr = tablerenderer.renderer(c)
                            appendFlow(cr.render(m.content[row], v, row, c))
                        }
                }
        }
        e.append(t)
    }

    fun renderCell(t: T, row: Int, col: String) {


    }
}

trait CellRenderer<T> {
    fun render(t : T, v : Any?, row : Int, col : String) : FlowContainer {
        if(v==null) return Span()
        var r : FlowContainer = Span()
        /*
        when(v) {
            is Int -> r.text("${v}")
            is Long -> r.text("${v}")
            is String -> r.text(v)
            else -> r.text("-")
        }
        */

        r.text("${v}")
        return r
    }
}



