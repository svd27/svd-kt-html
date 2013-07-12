package ch.passenger.kotlin.html.js.html

import ch.passenger.kotlin.html.js.model.Identifiable
import java.util.ArrayList
import java.util.HashSet
import js.jquery.jq

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

    override fun load(t: T) {
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

    override fun remove(t: T) {
        throw UnsupportedOperationException()
    }
    override fun delete(t: T) {
        throw UnsupportedOperationException()
    }
    override fun update(t: T, prop: String, old: Any?, nv: Any?) {
        throw UnsupportedOperationException()
    }

    override fun add(t: T) {
        throw UnsupportedOperationException()
    }

    abstract fun value(t: T, col: String): Any?
    abstract fun value(t: T, col: String, v: Any?)
}

class TableRenderer<T: Identifiable>(val selector: String, val model: TableModel<T>, val ids: String): TableListener<T> {
    override fun rowAdded(row: Int, t: T) {
        throw UnsupportedOperationException()
    }
    override fun rowRemove(row: Int, t: T) {
        throw UnsupportedOperationException()
    }
    override fun rowUpdated(row: Int, t: T) {
        throw UnsupportedOperationException()
    }
    override fun cellUpdated(row: Int, cell: Int, t: T, property: String, old: Any?, nv: Any?) {
        throw UnsupportedOperationException()
    }

    fun hid() :String {
        return ids
    }

    fun append(e: FlowContent) {
        e.table {
            atts {
                att("id", hid())
            }

            caption {
                text(model.title)
            }

            head {
                tr {
                    atts {
                        att("id", "${hid()}h")
                    }
                    for(c in model.columns) {
                        td {
                            atts { att("id", "${hid()}h${c}") }
                            text(c)
                        }
                    }
                }
            }

            body {
                for(row in 0..model.content.size())
                    tr {
                        atts {
                            att("id", "${hid()}r${row}")
                        }
                        for(c in model.columns)
                            td {
                                atts { att("id", "${hid()}r${row}c${c}") }
                                val s = model.value(model.content[row], c)?.toString()
                                if(s != null) text(s)
                            }
                    }
            }
        }
    }

    fun renderCell(t: T, row: Int, col: String) {
        val td = TableCell()
        val s = model.value(model.content[row], col)?.toString()

        td.atts{
            att("id", "${hid()}r${row}c${col}")
        }

        if(s != null) td.text(s)

        val has = jq("table#${hid()}").has("td#${hid()}r${row}c${col}")
        jq("table#${hid()} td#${hid()}r${row}c${col}").replaceWith(td.toString())
    }
}



