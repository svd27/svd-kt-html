package ch.passenger.kotlin.util.idea
import javax.swing.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.FlowLayout
import javax.swing.event.ListSelectionEvent

/**
 * Created with IntelliJ IDEA.
 * User: svd
 * Date: 31/07/13
 * Time: 07:57
 * To change this template use File | Settings | File Templates.
 */
class DepsUI() {
    fun show() {
        val tfSearch = textfield {
            setText("")
            setColumns(100)
        }
        val currentDep = label("")
        val tm = tablemodel<Artifact>() {
            cols = object : ColProvider<Artifact> {
                val cols = array("id", "group", "artifact", "version", "packaging")
                override fun get(col: Int): String {
                    return cols[col]
                }
                override fun count(): Int {
                    return cols.size
                }
            }

            vals = object : ValProvider<Artifact> {

                override fun value(t: Artifact, row: Int, col: Int): Any? {
                    return when(col) {
                        0 -> t.id
                        1 -> t.group
                        2 -> t.artifact
                        3 -> t.version
                        4 -> t.packaging
                        else -> "???"
                    }
                }
            }
        }

        val dtm = tablemodel<Artifact>() {
            cols = object : ColProvider<Artifact> {
                val cols = array("id", "group", "artifact", "version", "scope")
                override fun get(col: Int): String {
                    return cols[col]
                }
                override fun count(): Int {
                    return cols.size
                }
            }

            vals = object : ValProvider<Artifact> {

                override fun value(t: Artifact, row: Int, col: Int): Any? {
                    return when(col) {
                        0 -> t.id
                        1 -> t.group
                        2 -> t.artifact
                        3 -> t.version
                        4 -> t.scope
                        else -> "???"
                    }
                }
            }
        }

        val dtbl = JTable(dtm)

        val af = frame("search") {
            north {
                val p: JPanel = panel {
                    setLayout(FlowLayout())
                    add(label("Query (g:a:v)"))
                    add(tfSearch)
                    add(JButton(object : AbstractAction("search") {
                        public override fun actionPerformed(e: ActionEvent) {
                            val toks = tfSearch.getText()?.split(':')!!
                            if(toks.size == 0) return

                            val q = Array<String>(3) {
                                i ->
                                if(i < toks.size) toks[i] else ""
                            }


                            searchNexus(q[0], q[1], q[2], tm)
                        }
                    }))
                    this
                }
                p
            }
            center {
                scrollpane() {
                    val tbl = JTable(tm)


                    val condition = JComponent.WHEN_IN_FOCUSED_WINDOW
                    val inputMap = tbl.getInputMap(condition)
                    val actionMap = tbl.getActionMap()


                    inputMap?.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DEL");
                    actionMap?.put("DEL", object : AbstractAction() {
                        override public fun actionPerformed(e: ActionEvent) {
                            val sel  = tbl.getSelectedRows()
                            val ai = Array<Int>(sel.size) {sel[it]}

                            tm.remove(ai)
                        }
                    })

                    tbl.getSelectionModel()?.addListSelectionListener {
                        (e: ListSelectionEvent) ->
                        if(!e.getValueIsAdjusting()) {
                            val idx = tbl.getSelectedRow()
                            val artifact = tm.value(idx)
                            if(artifact != null) {
                                println(artifact.id)
                                dtm.clear()
                                currentDep.setText(artifact.id)
                                artifact.dependencies().forEach { dtm.add(it) }
                            }

                        }

                    }
                    tbl
                }
            }
            south() {
                vbox {
                    this + scrollpane() {
                        dtbl
                    }
                    this + hbox {
                        //this + label("Showing:")
                        this + currentDep
                        hglue()
                        add(JButton(object: AbstractAction("Promote") {

                            public override fun actionPerformed(e: ActionEvent) {
                                dtbl.getSelectedRows().forEach {
                                    val a = dtm.value(it)!!
                                    searchNexus(a.group, a.artifact, a.version, tm)
                                }
                            }
                        }))

                    }
                }
            }
        }
        af.pack()
        af.setVisible(true)
    }
}