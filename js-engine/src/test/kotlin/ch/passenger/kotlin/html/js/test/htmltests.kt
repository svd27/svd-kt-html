package ch.passenger.kotlin.html.js.test

import js.jquery.jq
import js.dom.html.window
import ch.passenger.kotlin.html.js.html.Div
import ch.passenger.kotlin.html.js.html.on
import ch.passenger.kotlin.html.js.html.MyWindow
import ch.passenger.kotlin.html.js.html.Callback
import ch.passenger.kotlin.html.js.Session
import js.debug.console
import java.util.ArrayList
import ch.passenger.kotlin.html.js.html.each
import ch.passenger.kotlin.html.js.html.*
import js.jquery.JQuery
import ch.passenger.kotlin.html.js.model.StringSelectionModel
import ch.passenger.kotlin.html.js.model.AbstractObserver
import ch.passenger.kotlin.html.js.model.AbstractSelectionModel
import ch.passenger.kotlin.html.js.html.svg.px
import ch.passenger.kotlin.html.js.html.svg.percent
import ch.passenger.kotlin.html.js.html.svg.NamedColor
import ch.passenger.kotlin.html.js.html.svg.ANamedColor
import js.dom.html.document
import org.w3c.dom.events.MutationEvent
import ch.passenger.kotlin.html.js.binding.MutationObserverInit
import ch.passenger.kotlin.html.js.binding.MutationRecord
import ch.passenger.kotlin.html.js.binding.*
import js.dom.core.Node
import ch.passenger.kotlin.html.js.worker.Worker
import ch.passenger.kotlin.html.js.html.svg.Grid
import ch.passenger.kotlin.html.js.html.svg.Cell
import ch.passenger.kotlin.html.js.model.Model
import ch.passenger.kotlin.html.js.model.Observer
import java.util.HashSet
import ch.passenger.kotlin.html.js.html.util.Converter
import js.dom.html.HTMLDivElement
import ch.passenger.kotlin.html.js.html.util.IntConverter
import ch.passenger.kotlin.html.js.model.SelectionModel
import ch.passenger.kotlin.html.js.html.svg.TrRotate
import ch.passenger.kotlin.html.js.html.svg.sec
import ch.passenger.kotlin.html.js.html.svg.TrTranslate
import ch.passenger.kotlin.html.js.logger.Logger
import ch.passenger.kotlin.html.js.html.components.ViewContainer
import ch.passenger.kotlin.html.js.html.components.TabbedView
import ch.passenger.kotlin.html.js.html.components.Gesture
import ch.passenger.kotlin.html.js.logger.LogFactory
import ch.passenger.kotlin.html.js.listOf

val SELF = window as Self

class A(val v: String, val d: Double) {
    fun toString(): String = "$v:$d"
}
class AConverter : Converter<A> {

    override fun convert2string(t: A): String {
        return "${t.v}=${t.d}"
    }
    override fun convert2target(s: String): A {
        throw UnsupportedOperationException()
    }
}

/**
 * Created by sdju on 16.08.13.
 */

public native fun document.addEventListener(kind: String, cb: (e: DOMEvent) -> Any?, f: Boolean): Unit = js.noImpl

public val log: Logger = LogFactory.logger("bosork-tests")

fun dump(n: Node) {
    console.log("Dump ${n.localName}:${n?.attributes?.getNamedItem("id")?.nodeValue}")
    n.childNodes.each {
        dump(it)
    }
}

var noiseStarted: Any? = null
var noiseLink: Link? = null

fun logNoise() {
    val d = Date(Date.now())
    log.debug("debugging lotsa noise at ", d)
    log.info("informing you, that you may be warned", d, " really i mean it!")
    log.warn("you had it coming...")
    log.error("now you did it! an error at exactly ${Date(Date.now())}")
    log.fatal("ive had enough, fatally annoyed with you, ", Date(Date.now()))
}


fun main(args: Array<String>) {
    //"DOMNodeInserted"
    console.log("################################# MAIN #################################")
    if(window.document == null) {
        console.log("worker mode ....")
        val mw = (window as MyWindow)!!
        mw.self?.postMessage("Worker started")

    } else initUI()

}

var currentCell: Model<Cell> = object : Model<Cell> {
    protected override val observers: MutableSet<Observer<Cell>> = HashSet()
    override var _value: Cell? = null
}


fun crteml(): MutableList<EntryMode> {
    val l = ArrayList<EntryMode>(EntryMode.values().size)
    EntryMode.values().each {
        l.add(it)
    }
    return l
}

val emlist: List<EntryMode> = crteml()
var selEM: Select<EntryMode>? = null

enum class EntryMode {
    SET CANDIDATE DELETE

    fun ints() {
        val l = ArrayList<Int>()
        values().each {
            l.add(it.ordinal())
        }
    }
}

class EntryModel() : AbstractSelectionModel<EntryMode>(crteml(), false) {
    class object {
        val converter: Converter<EntryMode> = object : Converter<EntryMode> {
            override fun convert2string(t: EntryMode): String = t.name()
            override fun convert2target(s: String): EntryMode = EntryMode.valueOf(s)
        }
    }
}
val modelEntry: EntryModel = EntryModel()

fun createNumbers(): MutableList<Int> {
    val l = ArrayList<Int>(9)
    for(i in 1..9) l.add(i)
    return l
}

val numbers: SelectionModel<Int> = object : AbstractSelectionModel<Int>(createNumbers(), false) {
}

//val entryValue : AbstractSelectionModel<Int> = object : AbstractSelectionModel<Int>(1..9,false) {}
var selEV: Select<Int>? = null

var theGrid: Grid? = null

var popUp: Div? = null

fun initUI() {
    jq {
        val mw = (window as MyWindow)!!
        mw.bosork = Session()
        val stringModel = StringSelectionModel(listOf("s", "v", "d"), false)
        val complexModel = object : AbstractSelectionModel<A>(listOf(A("s", 1.toDouble()), A("v", 2.toDouble()), A("d", 3.toDouble())), true) {
        }
        val coordInfoModel = CoordInfoModel()
        val parent = Div("root")
        val div = BorderLayout("content") {
            east {
                svg(100.px(), 100.px()) {
                    line(0.px(), 0.px(), 100.px(), 100.px()) {
                        stroke = black()
                        animate(TrRotate(0, 50, 50), TrRotate(360, 50, 50)) {
                            dur = 5.sec()
                            repeatCount = 3
                        }
                        animate(TrTranslate(0, 0), TrTranslate(50, 0)) {
                            dur = 5.sec()
                            repeatCount = 3
                            begin {
                                console.log("translate started")
                            }
                            end {
                                console.log("im done")
                                //parent?.detach()
                            }
                        }

                    }
                }

                val vc = ViewContainer()
                vc.div("a") {
                    text("A")
                }
                vc.div("b") {
                    text("B")
                }
                vc.div("c") {
                    text("C")
                }
                vc.addStyle("border", 3.px())
                +vc
                log.debug("vc.sel ${vc.view()}")
                div {
                    a("a") {
                        click {
                            log.debug("a select view -> ", vc.view())
                            vc.view("a")
                            log.debug("a select view -> ", vc.view())
                        }
                    }
                }
                div {
                    a("b") {
                        log.debug("b select view -> ", vc.view())
                        click { vc.view("b") }
                        log.debug("b select view -> ", vc.view())
                    }
                }
                div {
                    a("c") {
                        log.debug("c select view -> ", vc.view())
                        click { vc.view("c") }
                        log.debug("c select view -> ", vc.view())
                    }
                }

                val tv = TabbedView(Gesture.click, "tabbed")
                tv.cfg {
                    val h = Div("T1")
                    h.text("Tab 1")
                    val c = Div("C1")
                    c.div {
                        +BorderLayout() {
                            north {
                                text("north")
                            }
                            south {
                                text("south")
                            }
                            west {
                                text("west")
                            }
                        }
                    }
                    tab(h, c)
                    val csvg = Div("CSVG")
                    csvg.svg(100.px(), 100.px()) {
                        line(0.px(), 0.px(), 100.px(), 100.px()) {
                            stroke = black()
                            animate(TrRotate(0, 50, 50), TrRotate(360, 50, 50)) {
                                dur = 5.sec()
                                repeatCount = -1
                            }
                            animate(TrTranslate(0, 0), TrTranslate(50, 0)) {
                                dur = 5.sec()
                                repeatCount = 3
                                begin {
                                    console.log("translate started")
                                }
                                end {
                                    console.log("im done")
                                    //parent?.detach()
                                }
                            }

                        }
                    }
                    val h2 = Div("T2")
                    h2.text("SVG")
                    tab(h2, csvg)
                }
                +tv

            }
            north {
                div() {
                    console.log("model size: ${stringModel.items.size()}")
                    val obs = object : AbstractObserver<String>() {
                        override fun loaded(t: String) {
                            console.log("Selected: $t")
                            val a = A(t, 1.toDouble())
                            complexModel.add(a)
                            complexModel.select(a)
                        }
                        override fun unloaded(t: String) {
                            console.log("Unselected: $t")
                        }
                    }
                    stringModel.addObserver(obs)
                    select(stringModel) {
                        val cb = object :Callback {
                            override fun callback(event: DOMEvent) {
                                console.log("selected target: ", event.target.id)
                                console.log("selected target: ", event)
                                val t = jq("#${event.target.id}")
                                val sel = jq("#${event.target.id} option:selected")
                                console.log("selected jq: ", t)
                                console.log("selected jq selected: ", sel)
                                val el = event.data
                                console.log("event.data: ${event.data}: ${el}")
                            }
                        }
                    }
                }
                div("complex") {
                    text("complex")
                    console.log("model size: ${complexModel.items.size()}")
                    val obs = object : AbstractObserver<A>() {
                        override fun loaded(t: A) {
                            console.log("Selected: ${t.v}")
                        }
                        override fun unloaded(t: A) {
                            console.log("Unselected: ${t.v}")
                        }
                    }
                    complexModel.addObserver(obs)
                    select(complexModel, AConverter()) {
                        val cb = object :Callback {
                            override fun callback(event: DOMEvent) {
                                console.log("event.data: ${event.data}: ${event.data}")
                            }
                        }
                        //change(cb)
                    }
                }
                val divselection: Div = div("selection") {
                    span {
                        text("Whats selected")
                    }
                }
                complexModel.addObserver(object : AbstractObserver<A>(){
                    val divshow = divselection
                    fun show() {
                        divshow.clear()
                        complexModel.selections.each {
                            divshow.span() {
                                text("$it")
                                dirty = true
                            }
                        }
                    }
                    override fun loaded(t: A) {
                        console.log("$t selected")
                        show()
                    }
                    override fun unloaded(t: A) {
                        console.log("$t deselected")
                        show()
                    }
                })
                //CoordInfoComponent(div(){}, coordInfoModel)
            }

            center {
                div("svg") {
                    val that = this
                    atts {
                        //att("style", "width: 100%; height: 100%;")
                    }
                    val w = window as EventTarget
                    w.addEventListener("keypress", {
                        val ev = it as DOMKeyEvent

                        //val c = (0+ev.charCode).toChar()

                        val cc = it.charCode as Int
                        console.log("pressed ", it, " ", cc)
                        if(cc == 120) modelEntry.select(EntryMode.DELETE)
                        if(cc == 99) modelEntry.select(EntryMode.CANDIDATE)
                        if(cc == 118) modelEntry.select(EntryMode.SET)
                        console.log("${modelEntry.selections}")
                        if(cc > 48 && cc <= 57) {
                            val num = cc - 48
                            console.log("selecting $num")
                            numbers.select(num)
                            if(currentCell.value != null) {
                                var can: Boolean = modelEntry.firstSelected() != EntryMode.SET?:true
                                if(!can) can = theGrid?.validate(num, currentCell.value!!)?:true
                                else theGrid?.validate(num, currentCell.value!!)?:true
                                console.log("key manip ", num, " in ", currentCell?.value?.row, ",", currentCell?.value?.col,
                                        " mode ", modelEntry.firstSelected(), " can ", can)
                                if (can) {
                                    when(modelEntry.firstSelected()) {
                                        EntryMode.SET -> currentCell.value?.value(num)
                                        EntryMode.CANDIDATE -> currentCell?.value?.candidate(num)
                                        EntryMode.DELETE -> currentCell.value?.remove(num)
                                    }
                                    theGrid?.group?.dirty = true
                                }
                            }
                        }
                    }, false)

                    val svg = svg(500.px(), 500.px(), "enterrec") {
                        viewBox(0, 0, 500, 500)
                        pARnone()
                    }

                    val grid = Grid(svg, 500, 500, 9, 9, "grid")
                    theGrid = grid

                    grid.outerWidth = 3.px()
                    grid.legend = false
                    grid.paint()


                    grid.group?.mousemove {
                        e ->
                        val me = e as DOMMouseEvent
                        //console.log("sx: ${me.screenX} sy: ${me.screenY}")
                        //console.log("cx: ${me.clientX} cy: ${me.clientY}")
                        val svgp = grid.group?.client2locale(me.clientX, me.clientY)
                        //console.log("wx: ${svgp?.x} wy: ${svgp?.y}")
                        val cp = grid.cell(me)
                        //if(cp != null) console.log("update current: $cp")
                        currentCell.value = cp
                        //console.log("${cp.row},${cp.col}")

                        //coordInfoModel.value = CoordInfo(me.screenX, me.screenY, me.clientX, me.clientY, svgp!!.x, svgp!!.y)

                    }

                    grid.group?.click {
                        e ->
                        val me = e as DOMMouseEvent
                        if (!me.ctrlKey && !me.shiftKey) {
                            val c = grid.cell(me)
                            if (c != null) {
                                grid.group?.dirty = true
                                console.log("click button: ${e.button}")
                                console.log("click alt: ${e.altKey}  ctrl ${e.ctrlKey} shift ${e.shiftKey}")
                                val num = numbers.firstSelected()


                                var valid = true
                                val em = modelEntry.firstSelected()!!
                                if(theGrid != null && num != null && em == EntryMode.SET) {
                                    val ag = theGrid!!
                                    valid = ag.validate(num!!, c)
                                } else if(theGrid != null && num != null && em == EntryMode.CANDIDATE) {
                                    val ag = theGrid!!
                                    ag.validate(num!!, c)
                                }
                                console.log("click num ", num, " mode: ", modelEntry.firstSelected(), "valid ", valid)
                                if (num != null && valid) {
                                    when(modelEntry.firstSelected()) {
                                        EntryMode.SET -> c.value(num)
                                        EntryMode.CANDIDATE -> c.candidate(num)
                                        EntryMode.DELETE -> c.remove(num)
                                    }
                                }
                            }
                        } else if(me.ctrlKey) grid?.rotate(30)
                        else if(me.shiftKey) grid?.normTransform()
                    }
                    theGrid = grid
                }
            }
            west {
                val cdd = div() {

                }
                selEM = select(modelEntry, EntryModel.converter) { }
                modelEntry.select(EntryMode.SET)

                val cv = object : Converter<Int> {
                    val ic = IntConverter()
                    override fun convert2string(t: Int): String {
                        return ic.convert2string(t)
                    }
                    override fun convert2target(s: String): Int {
                        ic.convert2target(s)
                    }
                }

                selEV = select(numbers, cv) { }
                numbers.select(5)
                mouseover { currentCell.value = null }
                CellDisplay(cdd, currentCell)
                val opd = div() {
                    val model = object : Model<Boolean> {
                        override var _value: Boolean?
                            get() = theGrid?.legend
                            set(v) = if(theGrid != null) theGrid?.legend = v

                        protected override val observers: MutableSet<Observer<Boolean>> = HashSet()
                    }
                    model.addObserver(object:AbstractObserver<Boolean>(){
                        override fun added(t: Boolean) {
                            theGrid?.paint()
                        }
                        override fun deleted(t: Boolean) {
                            theGrid?.paint()
                        }
                        override fun updated(t: Boolean, prop: String, old: Any?, nv: Any?) {
                            theGrid?.paint()
                        }
                        override fun removed(t: Boolean) {
                            theGrid?.paint()
                        }
                    })
                    label("chkLegend") {
                        text("Legend")
                    }
                    checkbox(model, "chkLegend") { }
                }
                a("pop") {
                    click {
                        it.preventDefault()
                        popUp?.addStyle("display", "visible")
                        popUp?.dirty = true
                    }
                }
                noiseLink = a("start logging") {
                    click {
                        if(noiseStarted == null) {
                            noiseStarted = SELF.setInterval({ logNoise() }, 1000)
                            noiseLink?.clear()
                            noiseLink?.text("Stop IT!")
                        } else {
                            log.debug("stopping noise ", noiseStarted)
                            SELF.clearInterval(noiseStarted?:-1)
                            noiseStarted = null
                            noiseLink?.clear()
                            noiseLink?.text("start logging")
                        }
                        noiseLink?.dirty = true

                    }
                }
                a("shout log") {
                    click {
                        logNoise()
                    }
                }
            }
        }
        /*
        popUp = parent.div {
            addStyle("position","fixed")
            addStyle("display", "none")
            addStyle("background", "white")
            addStyle("opacity", ".8")
            addStyle("border", "3px")
            div {
                addChild(LogManager())
                a("Close") {
                    click {
                        popUp?.addStyle("display", "none")
                        popUp?.dirty = true
                    }
                }
            }


        }
        */

        parent.div("worker") {
            text("worker:")
            val SESSION = (window as MyWindow)!!.bosork!!
            val path = "${SESSION.base}/webworker"
            console.log("requesting worker on: $path")
            val w = Worker(path)
            val that = this
            w.onmessage = {
                e ->
                console.log("Worker said: ${e.data}")
                that.span() {
                    text("${e} ${e.data}")
                }
                that.dirty = true
            }
            console.log("starting worker", w)
            w.postMessage("start")
        }

        parent.addChild(div)

        val SESSION = (window as MyWindow)!!.bosork!!
        SESSION.root = parent
    }
}

class CellDisplay(val parent: FlowContainer, val model: Model<Cell>) : AbstractObserver<Cell>() {
    val div: Div = parent.div() { };
    {
        model.addObserver(this)
    }

    fun render() {
        if(model.value == null && div.node != null) {
            val n = div.node
            if(n != null) n.textContent = "-,-"
            //div.text("-,-")
        } else if(div.node != null) {
            val n = div.node
            if(n != null) n.textContent = "${model.value?.row},${model.value?.col}"
            //div.text("${model.value?.row},${model.value?.col}")
        }
        //div.dirty = true
    }

    override fun added(t: Cell) {
        console.log("CellDisplay add $t")
        render()
    }
    override fun removed(t: Cell) {
        console.log("CellDisplay remove $t")
        render()
    }
    override fun updated(t: Cell, prop: String, old: Any?, nv: Any?) {
        //        console.log("CellDisplay update $t")
        render()
    }
}

native fun Number.toFixed(n: Int): Number = js.noImpl



