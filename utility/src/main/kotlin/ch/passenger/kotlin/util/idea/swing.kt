package ch.passenger.kotlin.util.idea

import javax.swing.JFrame
import java.awt.Component
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.JTextArea
import javax.swing.table.TableModel
import javax.swing.table.AbstractTableModel
import javax.swing.JComponent
import java.util.ArrayList
import javax.swing.JViewport
import javax.swing.JScrollPane
import java.awt.Container
import javax.swing.Box

/**
 * Created by sdju on 29.07.13.
 */
fun frame(title:String, init: JFrame.()->Unit) : JFrame {
    val f = JFrame(title)
    f.init()
    return f
}

fun JFrame.center(c : ()->JComponent) {
    getContentPane()?.add(c(), BorderLayout.CENTER)
}

fun JFrame.north(c : ()->JComponent) {
    getContentPane()?.add(c(), BorderLayout.NORTH)
}

fun JFrame.south(c : ()->JComponent) {
    getContentPane()?.add(c(), BorderLayout.SOUTH)
}

fun vbox(init: Box.()->Unit) : Box {
    val vb = Box.createVerticalBox()
    vb.init()
    return vb
}

fun hbox(init: Box.()->Unit) : Box {
    val vb = Box.createHorizontalBox()
    vb.init()
    return vb
}

fun Box.vglue() {
    add(Box.createVerticalGlue())
}

fun Box.hglue() {
    add(Box.createHorizontalGlue())
}


fun JFrame.east(c : ()->JComponent) {
    getContentPane()?.add(c(), BorderLayout.EAST)
}

fun JFrame.west(c : ()->JComponent) {
    getContentPane()?.add(c(), BorderLayout.WEST)
}

fun panel(init:JPanel.()->Unit) : JPanel {
    val p :JPanel = JPanel(BorderLayout())
    p.init()
    return p
}

fun Container.plus(c: JComponent) {
    add(c)
}

fun label(l:String) : JLabel {
    return JLabel(l)
}

fun textfield(init:JTextField.()->Unit) : JTextField {
    val tf = JTextField()
    tf.init()
    return tf
}

fun textarea(init:JTextArea.()->Unit) : JTextArea {
    val tf = JTextArea()
    tf.init()
    return tf
}

fun scrollpane(init:JScrollPane.()-> JComponent) :JScrollPane {
    val sp = JScrollPane()
    sp.setViewportView(sp.init())
    return sp
}

fun<T:Comparable<T>> tablemodel(init:SimpleTableModel<T>.()->Unit) : SimpleTableModel<T> {
    val tm = SimpleTableModel<T>()
    tm.init()
    return tm
}


trait RowProvider<T:Comparable<T>> {
    fun get(row:Int) : T? = null
    fun add(t:T) : Int = -1
    fun count() : Int = 0
    fun clear()
    fun remove(t:T?) : Int = -1
    fun remove(r:Int) : Int {
        return remove(get(r))
    }
    fun indexOf(t:T) : Int = -1
}

fun<T> List<T>.eachIdx(cb:(Int,T)->Unit) : Unit {
    for(it in withIndices()) cb(it.first, it.second)
}

public class DefaultRowsProvider<T:Comparable<T>> : RowProvider<T> {
    private val rows : MutableList<T> = ArrayList<T>()


    override fun get(row: Int): T? {
        return rows.get(row)
    }
    override fun add(t: T): Int {
        rows.add(t)
        return count()-1
    }
    override fun count(): Int {
        return rows.size()
    }


    override fun indexOf(t: T): Int {
        var idx = -1
        rows.eachIdx {
            (i,ot) -> if(idx<0&&t.compareTo(ot)==0) idx = i
        }

        return idx
    }
    override fun clear() {
        rows.clear()
    }



    override fun remove(t:T?) :Int {
        if(t==null) return -1
        val idx = indexOf(t)
        rows.remove(idx)
        return idx
    }
}

trait ColProvider<T> {
    fun get(col:Int) : String {
        return "$col"
    }

    fun count() : Int = 0
}

trait ValProvider<T> {
    fun value(t:T, row:Int, col:Int) : Any? = null
}

class SimpleTableModel<T:Comparable<T>>() : AbstractTableModel() {
    var rows : RowProvider<T> = DefaultRowsProvider()
    var cols : ColProvider<T> = object : ColProvider<T>{}
    var vals : ValProvider<T> = object : ValProvider<T>{}


    public override fun getRowCount(): Int {
        return  rows.count()
    }
    public override fun getColumnCount(): Int {
        return cols.count()
    }

    public fun clear() {
        rows.clear()
        fireTableDataChanged()
    }
    public override fun getColumnName(column: Int): String {
        return cols.get(column)
    }

    public override fun getValueAt(row: Int, col: Int): Any? {
        val v = value(row)
        if(v==null) return "---"
        return  vals.value(v, row, col)
    }

    public fun add(t:T) {
        val idx = rows.add(t)
        if(idx<0) return
        fireTableRowsInserted(idx, idx)
    }

    public fun remove(irows:Array<Int>) {
        irows.forEach {
            val idx = rows.remove(value(it))
            fireTableRowsDeleted(idx, idx)
        }
    }

    public fun value(row:Int):T? = rows.get(row)


}

