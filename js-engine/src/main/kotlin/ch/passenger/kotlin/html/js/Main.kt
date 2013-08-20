package ch.passenger.kotlin.html.js

import js.*
import js.debug.console
import js.jquery.jq
import js.dom.html5.*
import js.dom.html.window
import ch.passenger.kotlin.html.js.binding.XMLHttpRequest
import js.jquery.JQuery
import ch.passenger.kotlin.html.js.html.Table
import ch.passenger.kotlin.html.js.model.Word
import java.util.HashMap
import js.dom.html.Event
import ch.passenger.kotlin.html.js.html.*
import ch.passenger.kotlin.html.js.model.WordTableModel
import js.dom.html.document
import ch.passenger.kotlin.html.js.binding.WebSocket
import ch.passenger.kotlin.html.js.html.*

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 24.06.13
 * Time: 19:30
 */

//public native fun JQuery.getJSON(url: String, data: Any?, success : (data: Any, status : String) -> Unit ) : Unit = js.noImpl

open public class Session {
    var base: String? = null
    val words = HashMap<Long, Word>()
    var root: FlowContainer = Div("xxxyyyxxx")
    set(r) {
        $root = r
        r.parent = ROOT_PARENT
        renderer.body(r)
    }
    var rootSelector: String = "body"
    var initialised = false
    val actionHolder = ActionHolder()
    var nextId = 0
    var token: Token? = null
    val renderer : Renderer = Renderer(window.document);

    {
        base = window.document.baseURI
        /*
        if(base?.endsWith("/")?:false) {
            val idx = window.document.baseURI.lastIndexOf("/")
            base = window.document.baseURI.substring(0, idx)
        }
        */

        console.log("!!!INIT SESSION!!! $base from ${window.document.baseURI}")
    }

    fun genId(): String {
        nextId = nextId + 1
        return "id${nextId}"
    }
    fun session_init() {
        if(initialised) return
        val load = Div("loader")
        load.text("loading")
        root = load
        initialised = true
    }




    fun fullRender() {
        renderer.render(root)
    }

    public fun refresh(el:HtmlElement) {
        renderer.render(el)
    }
    fun refresh() {
        console.log("session refresh")
        refresh(root)
    }
}



fun sendAjax(url: String, method: String, msg: String, cb: (req: XMLHttpRequest) -> Unit) {
    var req = XMLHttpRequest()

    req.onreadystatechange = {
        cb(req)
    }
    req.open(method, url)
    val mw = window as MyWindow
    val SESSION = mw.bosork!!
    if(SESSION.token != null) {
        req.setRequestHeader("BOSTOKEN", "${SESSION.token?.token}")
    }
    if(method == "GET")
        req.send()
    else
        req.send(msg)
}


fun mytable(json: Array<Definition>): Unit {
    val table: Table = Table("Table")

    table.caption {
        text(table.title)
    }

    table.body {
        for(el in json) {
            tr {
                td {
                    text(el.name)
                }
                td {
                    text(el.nick)
                }
            }
        }
    }


    val html = table.toString()
    jq("div#table").append(html)

}


native class Definition {
    val name: String = js.noImpl
    val nick: String = js.noImpl
}

native class Token {
    val token: String = js.noImpl
}

