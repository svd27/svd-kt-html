package ch.passenger.kotlin.html.js.test

import js.jquery.jq
import js.dom.html.window
import ch.passenger.kotlin.html.js.html.Div
import ch.passenger.kotlin.html.js.html.on
import ch.passenger.kotlin.html.js.html.MyWindow
import ch.passenger.kotlin.html.js.html.DOMEvent
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

public native fun document.addEventListener(kind: String, cb: (e: DOMEvent)->Any?, f: Boolean): Unit = js.noImpl

fun dump(n: Node) {
    console.log("Dump ${n.localName}:${n?.attributes?.getNamedItem("id")?.nodeValue}")
    n.childNodes.each {
        dump(it)
    }
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

val emlist : List<EntryMode> = crteml()
var selEM : Select<EntryMode,MutableList<EntryMode>>?= null

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
        val converter : Converter<EntryMode> = object : Converter<EntryMode> {
            override fun convert2string(t: EntryMode): String = t.name()
            override fun convert2target(s: String): EntryMode = EntryMode.valueOf(s)
        }
    }
}
val modelEntry : EntryModel = EntryModel()

fun createNumbers() : MutableList<Int> {
    val l = ArrayList<Int>(9)
    for(i in 1..9) l.add(i)
    return l
}

val numbers : SelectionModel<Int,MutableList<Int>> = object : AbstractSelectionModel<Int>(createNumbers(), false) { }

//val entryValue : AbstractSelectionModel<Int> = object : AbstractSelectionModel<Int>(1..9,false) {}
var selEV : Select<Int,MutableList<Int>>? = null

var theGrid: Grid? = null

fun initUI() {
    jq {
        val mw = (window as MyWindow)!!
        mw.bosork = Session()
        val stringModel = StringSelectionModel(listOf("s", "v", "d"), false)
        val complexModel = object : AbstractSelectionModel<A>(listOf(A("s", 1.toDouble()), A("v", 2.toDouble()), A("d", 3.toDouble())), true) {
        }
        val coordInfoModel = CoordInfoModel()
        val div = BorderLayout("content") {
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
                        if(cc ==120) modelEntry.select(EntryMode.DELETE)
                        if(cc ==99) modelEntry.select(EntryMode.CANDIDATE)
                        if(cc ==118) modelEntry.select(EntryMode.SET)
                        console.log("${modelEntry.selections}")
                        if(currentCell.value != null) {
                            if(cc > 48 && cc <= 57) {
                                val num = cc - 48
                                console.log("selecting $num")
                                numbers.select(num)

                                console.log("key manip ", num, " in ", currentCell?.value?.row, ",", currentCell?.value?.col,
                                        " mode ", modelEntry.firstSelected())
                                when(modelEntry.firstSelected()) {
                                    EntryMode.SET -> currentCell.value?.value(num)
                                    EntryMode.CANDIDATE -> currentCell?.value?.candidate(num)
                                    EntryMode.DELETE -> currentCell.value?.remove(num)
                                }
                                theGrid?.group?.dirty = true
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
                        val c = grid.cell(me)
                        if (c != null) {
                            grid.group?.dirty = true
                            console.log("click button: ${e.button}")
                            console.log("click alt: ${e.altKey}  ctrl ${e.ctrlKey} shift ${e.shiftKey}")
                            val num = numbers.firstSelected()
                            console.log("click num ", num, " mode: ", modelEntry.firstSelected())

                            if (num!=null) {
                                when(modelEntry.firstSelected()) {
                                    EntryMode.SET -> currentCell.value?.value(num)
                                    EntryMode.CANDIDATE -> currentCell?.value?.candidate(num)
                                    EntryMode.DELETE -> currentCell.value?.remove(num)
                                }
                            }
                        }
                    }
                    theGrid = grid
                }
            }
            west {
                val cdd = div() {

                }
                selEM = select(modelEntry, EntryModel.converter) {}
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

                selEV = select(numbers,cv){}
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
            }
        }

        /*
                div.div("worker") {
                    //TODO: cant call text inside each
                    val SESSION = (window as MyWindow)!!.bosork!!
                    val path = "${SESSION.base}/webworker"
                    console.log("requesting worker on: $path")
                    val w = Worker(path)
                    w.onmessage = {
                        e ->
                        val SESSION = (window as MyWindow)!!.bosork!!
                        val wdiv = SESSION.root.find("worker") as FlowContainer
                        console.log("Worker said: ${e.data}")
                        wdiv.span() {
                            text("${e} ${e.data}")
                        }
                    }
                    console.log("starting worker", w)
                    w.postMessage("start")
                }
        */


        val SESSION = (window as MyWindow)!!.bosork!!
        SESSION.root = div
    }
}

class CellDisplay(val parent: FlowContainer, val model: Model<Cell>) : AbstractObserver<Cell>() {
    val div: Div = parent.div() { };
    {
        model.addObserver(this)
    }

    fun render() {
        div.clear()
        if(model.value == null) {
            div.text("-,-")
        } else {
            div.text("${model.value?.row},${model.value?.col}")
        }
        div.dirty = true
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

class CoordInfoComponent(private val parent: FlowContainer, private val model: CoordInfoModel) : AbstractObserver<CoordInfo>() {
    var screenX: Span? = null
    var screenY: Span? = null
    var clientX: Span? = null
    var clientY: Span? = null
    var svgX: Span? = null
    var svgY: Span? = null

    {
        model.addObserver(this)
        val m = model
        val that = this
        parent.div {
            div {
                div {
                    text("screen")
                    that.screenX = span {
                        if(m.value?.screenX != null)
                            text("x(${m.value?.screenX?.toFixed(2)})")
                    }
                    that.screenY = span {
                        if(m.value?.screenY != null)
                            text("y(${m.value?.screenY?.toFixed(2)})")
                    }
                }
                div{
                    text("client")
                    that.clientX = span {
                        if(m.value?.clientX != null)
                            text("x(${m.value?.clientX?.toFixed(2)})")
                    }
                    that.clientY = span {
                        if(m.value?.clientY != null)
                            text("y(${m.value?.clientY?.toFixed(2)})")
                    }
                }
                div {
                    text("svg")
                    that.svgX = span {
                        if(m.value?.svgX != null)
                            text("x(${m.value?.svgX?.toFixed(2)})")
                    }

                    that.svgY = span {
                        if(m.value?.svgY != null)
                            text("y(${m.value?.svgY?.toFixed(2)})")
                    }
                }
            }
        }
    }


    override fun added(t: CoordInfo) {
        all()
    }
    override fun removed(t: CoordInfo) {
        all()
    }
    override fun updated(t: CoordInfo, prop: String, old: Any?, nv: Any?) {
        all()
    }

    fun all() {
        rCX(); rCY(); rSX(); rSY(); rVX(); rVY()
    }

    fun rCX() {
        clientX?.clear()
        clientX?.text("x(${model.value?.clientX}(")
        clientX?.dirty = true
    }

    fun rCY() {
        clientY?.clear()
        clientY?.text("y(${model.value?.clientY})")
        clientY?.dirty = true
    }

    fun rSX() {
        screenX?.clear()
        screenX?.text("x(${model.value?.screenX})")
        screenX?.dirty = true
    }

    fun rSY() {
        screenY?.clear()
        screenY?.text("y(${model.value?.screenY})")
        screenY?.dirty = true
    }

    fun rVX() {
        svgX?.clear()
        svgX?.text("x(${model.value?.svgX})")
        svgX?.dirty = true
    }

    fun rVY() {
        svgY?.clear()
        svgY?.text("y(${model.value?.svgY}(")
        svgY?.dirty = true
    }
}

class CoordInfoModel() : Model<CoordInfo> {
    protected override val observers: MutableSet<Observer<CoordInfo>> = HashSet()
    protected override var _value: CoordInfo? = null


    protected override fun checkValue(nv: CoordInfo?, ov: CoordInfo?): CoordInfo? {
        if(ov == null) {
            fireAdd(nv!!)
        }
        if(nv == null) {
            fireRemove(ov!!)
            fireDelete(ov!!)
        }

        val oc = ov!!
        val nc = nv!!

        if(oc.screenX != nc.screenX) {
            fireUpdate(nc, "screenX", oc.screenX, nc.screenX)
        }

        if(oc.screenY != nc.screenY) {
            fireUpdate(nc, "screenY", oc.screenY, nc.screenY)
        }

        if(oc.clientX != nc.clientX) {
            fireUpdate(nc, "clientX", oc.clientX, nc.clientX)
        }

        if(oc.clientY != nc.clientY) {
            fireUpdate(nc, "clientY", oc.clientY, nc.clientY)
        }

        if(oc.svgX != nc.svgX) {
            fireUpdate(nc, "svgX", oc.svgX, nc.svgX)
        }

        if(oc.svgY != nc.svgY) {
            fireUpdate(nc, "svgY", oc.svgY, nc.svgY)
        }

        return nv
    }
}

class CoordInfo(var screenX: Number, var screenY: Number, var clientX: Number, var clientY: Number, var svgX: Number, var svgY: Number)