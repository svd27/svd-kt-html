package ch.passenger.kotlin.util.idea
import javax.swing.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.FlowLayout
import javax.swing.event.ListSelectionEvent
import java.util.ArrayList

/**
 * Created with IntelliJ IDEA.
 * User: svd
 * Date: 31/07/13
 * Time: 07:57
 * To change this template use File | Settings | File Templates.
 */
var packButton = JButton()
class DepsUI() {
    var tfIdea : JTextField? = null
    var tfLibs : JTextField? = null
    var tfName : JTextField? = null
    var tfParent : JTextField? = null
    var tbl = JTable()
    var af = JFrame()

    private var _title : String = ""
    var title : String
            get() {return _title }
            set(t) {
                _title = t
                af.setTitle(t)
            }

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


    fun show() {
        val tfSearch = textfield {
            setText("")
            setColumns(100)
        }
        val currentDep = label("")

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

        af = frame("search") {
            north {
                vbox {
                    this + panel {
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


                                searchNexus(q[0], q[1], q[2]).forEach {
                                    tm.add(it)
                                }
                            }
                        }))
                        this
                    }
                    this + hbox {
                        this + hbox {
                            this +label("Project Folder:")
                            this +textfield {
                                tfIdea = this
                                setColumns(50)
                                setText(System.getProperty("user.dir"))
                            }
                            this + JButton(object : AbstractAction("...") {
                                val jfc = JFileChooser(System.getProperty("user.dir"))
                                public override fun actionPerformed(e: ActionEvent) {
                                    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
                                    val ok = jfc.showOpenDialog(tfIdea)
                                    if(ok == JFileChooser.APPROVE_OPTION)
                                        tfIdea?.setText(jfc.getSelectedFile()?.getAbsolutePath())
                                }
                            })
                        }
                        this + hbox {
                            this +label("artifact folder")
                            this +textfield {
                                tfLibs = this
                                setColumns(50)
                                setText("lib")
                            }

                        }
                        this + hbox {
                            this+label("Parent")
                            this+textfield {
                                tfParent = this
                                setColumns(20)
                            }
                            this+JButton(object:AbstractAction("Clear") {

                                public override fun actionPerformed(e: ActionEvent) {
                                    tm.clear()
                                }
                            })
                        }
                        hglue()
                    }
                }
            }
            center {
                vbox {
                    this + scrollpane() {
                        tbl = JTable(tm)
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
                                    tfParent?.setText(artifact.parentName())
                                    dtm.clear()
                                    currentDep.setText(artifact.id)
                                    artifact.dependencies().forEach { dtm.add(it) }
                                }

                            }

                        }
                        tbl
                    }
                    this + hbox {
                        this + JButton(object : AbstractAction("Download JAR"){
                            public override fun actionPerformed(e: ActionEvent) {
                                val sel = tbl.getSelectedRows()
                                sel.forEach {
                                    tm.value(it)?.download(tfIdea?.getText()+"/"+tfLibs?.getText()!!)
                                }
                            }
                        })

                        this + JButton(object : AbstractAction("Package"){
                            public override fun actionPerformed(e: ActionEvent) {
                                val sel = tbl.getSelectedRows()
                                val artifacts = ArrayList<Artifact>(sel.size)
                                sel.forEach {
                                    artifacts.add(tm.value(it)!!)
                                }
                                val pf = CfgFrame(artifacts, tfLibs?.getText()!!, tfIdea?.getText()!!)
                                pf.show()
                            }
                        })

                        this + JButton(object : AbstractAction("Load Cfg..."){
                            val jfc = JFileChooser(System.getProperty("user.dir"))
                            public override fun actionPerformed(e: ActionEvent) {
                                val ok = jfc.showOpenDialog(af)
                                if(ok==JFileChooser.APPROVE_OPTION) {
                                    val f = jfc.getSelectedFile()
                                    val cfg = readCfg(f!!)

                                    val ui = DepsUI()
                                    ui.title = cfg.leader.id
                                    ui.tm.addAll(cfg.artifacts)
                                    ui.show()
                                    CfgFrame(cfg.artifacts, cfg.libDir, tfIdea?.getText()!!).show()
                                }
                            }
                        })

                        this + JButton(object : AbstractAction("Focus on Deps"){
                            public override fun actionPerformed(e: ActionEvent) {
                                val sel = tbl.getSelectedRows()

                                sel.forEach {
                                    val a = tm.value(it)!!
                                    val dui = DepsUI()
                                    dui.tm.addAll(a.dependencies())
                                    dui.show()
                                    dui.title = "Dependencies of: ${a.id}"
                                }

                            }
                        })

                        this + JButton(object : AbstractAction("POM") {
                            public override fun actionPerformed(e: ActionEvent) {
                                val sel = tbl.getSelectedRows()
                                sel.forEach {
                                    val a = tm.value(it)!!
                                    val f = frame("POM: ${a.id}") {
                                        center {
                                            scrollpane {
                                                JEditorPane("text/xml", a.getPOMDoc().asXML())
                                            }
                                        }
                                    }
                                    f.pack()
                                    f.setVisible(true)
                                }

                            }
                        })
                    }
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
                                    val l = searchNexus(a.group, a.artifact, a.version)
                                    l.forEach {
                                        tm.add(it)
                                    }
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

class CfgFrame(val artifacts:List<Artifact>, val libDir:String, val projectDir:String) {
    var leader = artifacts.first()
    val tfLeader = textfield {
        setText(artifacts.first().id)
    }
    val tfProject = textfield {setText(projectDir)}

    val tm = tablemodel<Artifact> {

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

        artifacts.forEach { add(it) }
    }

    val tbl = JTable(tm)


    public fun show() {
        val f = frame("Configuration for ${leader.id}") {
            north {
                vbox {
                    this+hbox {
                        this+label("Leader:")
                        this+tfLeader
                        this+JButton(object:AbstractAction("Promote Selected"){
                            public override fun actionPerformed(e: ActionEvent) {
                                val sel = tbl.getSelectedRows()
                                if(sel.size==1) {
                                    leader = tm.value(sel[0])!!
                                    tfLeader.setText(leader.id)
                                }
                            }
                        })
                    }
                    this+hbox {
                        this+label("Project Dir:")
                        this+textfield {
                            setText(projectDir)
                            setEnabled(false)
                        }
                        this+label("Lib Dir:")
                        this+textfield {
                            setText(libDir)
                            setEnabled(false)
                        }
                    }
                    this+hbox {
                        packButton = JButton(object:AbstractAction("Download and Create Lib"){
                            public override fun actionPerformed(e: ActionEvent) {
                                packButton.setEnabled(false)
                                val w = object : SwingWorker<LibCfg,Void>() {
                                    protected override fun doInBackground(): LibCfg? {
                                        try {
                                            val cfg = LibCfg(leader, artifacts, libDir)
                                            pack(cfg, projectDir, packButton)
                                            return cfg
                                        } finally {
                                            SwingUtilities.invokeLater {
                                                packButton.setEnabled(true)
                                            }
                                        }
                                    }
                                }
                                w.execute()
                            }
                        })
                        this + packButton
                        val bsave = JButton(object:AbstractAction("Save..."){
                            val jfc = JFileChooser()

                            public override fun actionPerformed(e: ActionEvent) {
                                val ok = jfc.showOpenDialog(tfProject)
                                if(ok==JFileChooser.APPROVE_OPTION) {
                                    saveCfg(jfc.getSelectedFile()!!, LibCfg(leader, artifacts, libDir))
                                }
                            }
                        })
                        this + bsave
                    }
                }
            }

            center {
                scrollpane {
                    tbl
                }
            }

        }
        f.pack()
        f.setVisible(true)
    }
}