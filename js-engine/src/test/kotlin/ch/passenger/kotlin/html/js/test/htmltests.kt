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
fun main(args: Array<String>) {
    jq {
        val mw = (window as MyWindow)!!

        if(mw.bosork==null) {
            mw.bosork = Session()
            jq("body").on("click", "a.action") {
                event ->
                mw.bosork!!.actionHolder.trigger(event)
            }
            jq("body").on("change", "select.action") {
                event ->
                mw.bosork!!.actionHolder.trigger(event)
            }
            val title = jq("html head title")
            title.text("Words")
            jq("div#uri").text(window.document.baseURI)

            var idx = window.document.baseURI.lastIndexOf("/")
            mw.bosork!!.base = window.document.baseURI.substring(0, idx)
            //mw.bosork!!.session_init()
        }

        val body = jq("body")

        val div = Div("content")
        val stringModel = StringSelectionModel(listOf("s", "v", "d"), false)
        val complexModel = object : AbstractSelectionModel<A>(listOf(A("s", 1.toDouble()), A("v", 2.toDouble()), A("d", 3.toDouble())), false) {}
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

        body.html(div.render())
        val SESSION = (window as MyWindow)!!.bosork!!
        SESSION.root = div

    }
}