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

class A(val v : String, val d:Double) {
    fun toString() : String = "$v:$d"
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

public native fun document.addEventListener(kind:String, cb : (e:DOMEvent)->Any?, f:Boolean) : Unit = js.noImpl

fun dump(n:Node) {
    console.log("Dump ${n.localName}:${n?.attributes?.getNamedItem("id")?.nodeValue}")
    n.childNodes.each {
        dump(it)
    }
}

fun muadded(added:Node) {
    console.log("MU ADD ${added.localName}")
    val mocfg = A("x",1.0) as MutationObserverInit
    mocfg.childList = true
    mocfg.subtree = true

    val mocb = {
        (recs:Array<MutationRecord>) ->

        recs.each {
            console.log("MUTATION ${it.`type`}")
            it.addedNodes.each {
                //dump(it)
                muadded(it)
            }
        }
    }

    console.log("observe ${added.attributes.getNamedItem("id")?.nodeValue}")
    val mobs = MutationObserver(mocb)
    mobs.observe(added, mocfg)
}


fun main(args: Array<String>) {
    //"DOMNodeInserted"

    val cb = {(e:DOMEvent) -> {
        console.log("DOM STRUCTURE ${e.target}")
    }};
    val et = window.document as EventTarget
    et.addEventListener("DOMNodeInserted", cb, false)
    //window.document.addEventListener("DOMNodeInserted", cb, false)

    jq {
        val mw = (window as MyWindow)!!

        if(mw.bosork==null) {
            mw.bosork = Session()
            jq("body").on("click", ".action") {
                event ->
                mw.bosork!!.actionHolder.action(event)
            }
            jq("body").on("change", ".change") {
                event ->
                mw.bosork!!.actionHolder.action(event)
            }
            jq("body").on("mouseenter", ".mouseenter") {
                event ->
                console.log("main.enter")
                mw.bosork!!.actionHolder.enter(event)
            }
            jq("body").on("mouseleave", ".mouseleave") {
                event ->
                console.log("main.leave")
                mw.bosork!!.actionHolder.leave(event)
            }
            jq("body").on("DOMNodeInsertedIntoDocument") {
                console.log("DOM modified $it ${it.target.id}")
            }
            val e = window.document.getElementById("svg")
            val title = jq("html head title")
            title.text("Words")
            jq("div#uri").text(window.document.baseURI)


            var idx = window.document.baseURI.lastIndexOf("/")
            mw.bosork!!.base = window.document.baseURI.substring(0, idx)
            //mw.bosork!!.session_init()
        }
        val bodynl = window.document.getElementsByTagName("body")
        val bt = bodynl.item(0)
        /*
        val mocfg = MutationObserverInit()
        mocfg.childList = true
        mocfg.subtree = true
        */
        val mocfg = A("x",1.0) as MutationObserverInit
        mocfg.childList = true
        mocfg.subtree = true

        val mocb = {
            (recs:Array<MutationRecord>) ->

            jq(".mouseenter").mouseenter {
                event ->
                console.log("mouseenter main.enter")
                mw.bosork!!.actionHolder.enter(event)
            }

            jq(".mouseleave").mouseleave() {
                event ->
                console.log("mouseleave main.leave")
                mw.bosork!!.actionHolder.leave(event)
            }

            recs.each {
                console.log("ROOT MUTATION ${it.`type`}")
                it.addedNodes.each {
                    //dump(it)
                    muadded(it)
                }
            }
        }
        val mobs = MutationObserver(mocb)

        console.log("MU start root observer on $bt")
        if(bt!=null)
            mobs.observe(bt, mocfg)

        val body = jq("body")

        val div = Div("content")
        val stringModel = StringSelectionModel(listOf("s", "v", "d"), false)
        val complexModel = object : AbstractSelectionModel<A>(listOf(A("s", 1.toDouble()), A("v", 2.toDouble()), A("d", 3.toDouble())), true) {}
        div.div("") {
            text("hi")

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
        div.div("complex") {
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
        val divselection : Div = Div("selection")
        divselection.span {
            text("Whats selected")
        }
        div.appendFlow(divselection)

        complexModel.addObserver(object : AbstractObserver<A>(){
            val divshow = divselection
            {
                console.log("showing selections in ${divshow.id()}")
            }
            fun show() {
                console.log("showing selections in ${divshow.id()}")
                divshow.clear()
                complexModel.selections.each {
                    divshow.span {
                        text("$it")
                    }
                }
                //val SESSION = (window as MyWindow)!!.bosork!!
                //SESSION.refresh(divshow)
                divshow.dirty = true
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

        div.div("svg") {
            svg(percent(100), percent(100), "enterrec") {
                rect(px(10), px(10), px(90), px(90)) {
                    fill(ANamedColor("magenta"))
                    stroke(ANamedColor("grey"))
                    mouseenter(object:Callback {
                        override fun callback(event: DOMEvent) {
                            console.log("enter")
                            fill(ANamedColor("red"))
                            val SESSION = (window as MyWindow)!!.bosork!!
                            val me = SESSION.root.find("enterrec")
                            me?.dirty = true
                        }
                    })
                    /*
                    click(object:Callback {
                        override fun callback(event: DOMEvent) {
                            console.log("click")
                            fill(ANamedColor("red"))
                            val SESSION = (window as MyWindow)!!.bosork!!
                            val me = SESSION.root.find("enterrec")
                            me?.dirty = true
                        }
                    })*/
                    mouseleave(object:Callback {
                        override fun callback(event: DOMEvent) {
                            console.log("leave")
                            fill(ANamedColor("magenta"))
                            val SESSION = (window as MyWindow)!!.bosork!!
                            val me = SESSION.root.find("enterrec")
                            me?.dirty = true
                        }
                    })
                }
            }
        }


        body.html(div.render())
        val SESSION = (window as MyWindow)!!.bosork!!
        SESSION.root = div

    }
}