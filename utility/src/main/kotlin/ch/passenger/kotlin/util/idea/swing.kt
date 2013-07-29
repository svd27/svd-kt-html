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

fun<T> tablemodel(init:SimpleTableModel<T>.()->Unit) : SimpleTableModel<T> {
    val tm = SimpleTableModel<T>()
    tm.init()
    return tm
}


trait RowProvider<T> {
    fun get(row:Int) : T? = null
    fun count() : Int = 0
}

trait ColProvider<T> {
    fun get(col:Int) : String {
        return "$col"
    }

    fun count() : Int = 0
}

trait ValProvider<T> {
    fun value(row:Int, col:Int) : Any? = null
}

class SimpleTableModel<T>() : AbstractTableModel() {
    var rows : RowProvider<T> = object : RowProvider<T>{}
    var cols : ColProvider<T> = object : ColProvider<T>{}
    var vals : ValProvider<T> = object : ValProvider<T>{}


    public override fun getRowCount(): Int {
        return  rows.count()
    }
    public override fun getColumnCount(): Int {
        return cols.count()
    }
    public override fun getValueAt(row: Int, col: Int): Any? {
        return  vals.value(row, col)
    }
}

