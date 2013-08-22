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
                CoordInfoComponent(div(){}, coordInfoModel)
            }

            center {
                div("svg") {
                    atts {
                        //att("style", "width: 100%; height: 100%;")
                    }
                    val svg = svg(500.px(), 500.px(), "enterrec") {
                        viewBox(0, 0, 500, 500)
                        pARnone()
                        /*
                                                rect(px(10), px(10), px(90), px(90), "rect") {
                                                    fill(ANamedColor("magenta"))
                                                    stroke(ANamedColor("grey"))
                                                    val r = this
                                                    mouseenter {
                                                        console.log("enter")
                                                        r.fill(ANamedColor("red"))
                                                        r.dirty = true
                                                    }


                                                    mouseleave {
                                                        console.log("leave")
                                                        r.fill(ANamedColor("magenta"))
                                                        r.dirty = true
                                                    }

                                                    click {
                                                        console.log("click")
                                                        r.fill(ANamedColor("peach"))
                                                        r.dirty = true
                                                    }
                                                }
                        */
                    }

                    val grid = Grid(svg, 300, 300, 9, 9, "grid")

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
                        if(cp != null) console.log("update current: $cp")
                        currentCell.value = cp
                        //console.log("${cp.row},${cp.col}")

                        coordInfoModel.value = CoordInfo(me.screenX, me.screenY, me.clientX, me.clientY, svgp!!.x, svgp!!.y)

                    }

                    grid.group?.click {
                        e ->
                        val me = e as DOMMouseEvent
                        val c = grid.cell(me)
                        if (c != null) {
                            console.log("click button: ${e.button}")
                            grid.cellTextCenter(c) {
                                attribute("opacity", "0.6")
                                stroke = ANamedColor("red")
                                text("${c.row}:${c.col}")
                            }
                            grid.group?.dirty = true
                            if(e.button == 1.toShort()) {

                            } else {
                                //c.ne("1")
                            }
                        }
                    }
                    theGrid = grid
                }
            }
            west {
                val cdd = div() {

                }
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
                            theGrid?.group?.dirty = true
                        }
                        override fun deleted(t: Boolean) {
                            theGrid?.group?.dirty = true
                        }
                        override fun updated(t: Boolean, prop: String, old: Any?, nv: Any?) {
                            theGrid?.group?.dirty = true
                        }
                        override fun removed(t: Boolean) {
                            theGrid?.group?.dirty = true
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
        console.log("CellDisplay update $t")
        render()
    }
}

native fun Number.toFixed(n:Int):Number = js.noImpl

class CoordInfoComponent(private val parent:FlowContainer, private val model:CoordInfoModel) : AbstractObserver<CoordInfo>() {
    var screenX : Span? = null
    var screenY : Span? = null
    var clientX : Span? = null
    var clientY : Span? = null
    var svgX : Span? = null
    var svgY : Span? = null

    {
        model.addObserver(this)
        val m = model
        val that = this
        parent.div {
            div {
                div {
                    text("screen")
                    that.screenX = span {
                        if(m.value?.screenX!=null)
                        text("x(${m.value?.screenX?.toFixed(2)})")
                    }
                    that.screenY = span {
                        if(m.value?.screenY!=null)
                        text("y(${m.value?.screenY?.toFixed(2)})")
                    }
                }
                div{
                    text("client")
                    that.clientX = span {
                        if(m.value?.clientX!=null)
                        text("x(${m.value?.clientX?.toFixed(2)})")
                    }
                    that.clientY = span {
                        if(m.value?.clientY!=null)
                        text("y(${m.value?.clientY?.toFixed(2)})")
                    }
                }
                div {
                    text("svg")
                    that.svgX = span {
                        if(m.value?.svgX!=null)
                        text("x(${m.value?.svgX?.toFixed(2)})")
                    }

                    that.svgY = span {
                        if(m.value?.svgY!=null)
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
        if(ov==null) {
            fireAdd(nv!!)
        }
        if(nv==null) {
            fireRemove(ov!!)
            fireDelete(ov!!)
        }

        val oc = ov!!
        val nc = nv!!

        if(oc.screenX!=nc.screenX) {
            fireUpdate(nc, "screenX", oc.screenX, nc.screenX)
        }

        if(oc.screenY!=nc.screenY) {
            fireUpdate(nc, "screenY", oc.screenY, nc.screenY)
        }

        if(oc.clientX!=nc.clientX) {
            fireUpdate(nc, "clientX", oc.clientX, nc.clientX)
        }

        if(oc.clientY!=nc.clientY) {
            fireUpdate(nc, "clientY", oc.clientY, nc.clientY)
        }

        if(oc.svgX!=nc.svgX) {
            fireUpdate(nc, "svgX", oc.svgX, nc.svgX)
        }

        if(oc.svgY!=nc.svgY) {
            fireUpdate(nc, "svgY", oc.svgY, nc.svgY)
        }

        return nv
    }
}

class CoordInfo(var screenX:Number, var screenY:Number, var clientX:Number, var clientY:Number, var svgX:Number, var svgY:Number)