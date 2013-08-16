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

class A(val v : String)

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
        div.div("") {
            text("hi")
            select() {
                val cb = object :Callback {
                    override fun callback(event: DOMEvent) {
                        console.log("selected target: ", event.target.id)
                        console.log("selected target: ", event)
                        val t = jq("#${event.target.id}")
                        val sel = jq("#${event.target.id} option:selected")
                        console.log("selected jq: ", t)
                        console.log("selected jq selected: ", sel)
                    }
                }
                change(cb)
                var o = A("s")
                option(o, o.v) {
                    label(o.v)
                    value(o.v)
                    selected(true)
                }
                o = A("v")
                option(o, o.v) {
                    label(o.v)
                    value(o.v)
                }
                o = A("d")
                option(o, o.v) {
                    label(o.v)
                    value(o.v)
                }
            }
        }

        body.html(div.render())

    }
}