package ch.passenger.kotlin.html.puzzles.frontend.web

import js.jquery.jq
import js.dom.html.window
import ch.passenger.kotlin.html.js.html.Div
import ch.passenger.kotlin.html.js.html.on
import ch.passenger.kotlin.html.js.html.MyWindow
import ch.passenger.kotlin.html.js.html.DOMEvent
import ch.passenger.kotlin.html.js.html.Callback
import ch.passenger.kotlin.html.js.Session

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
            mw.bosork!!.session_init()
        }

        val body = jq("body")

        val div = Div("content").div("") {
            text("hi")
        }


    }
}