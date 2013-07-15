package ch.passenger.kotlin.html.js

import js.*
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

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 24.06.13
 * Time: 19:30
 */

//public native fun JQuery.getJSON(url: String, data: Any?, success : (data: Any, status : String) -> Unit ) : Unit = js.noImpl

open public class Session {
    var base : String? = null
    val words = HashMap<Long,Word>()
    var root : FlowContainer = Div("xxxyyyxxx")
    var rootSelector : String = "body"
    var initialised = false
    val actionHolder = ActionHolder()
    var nextId = 0

    fun genId() :String {
        nextId = nextId+1
        return "id${nextId}"
    }
    fun session_init() {
        if(initialised) return
        val load = Div("loader")
        load.text("loading")
        jq("body").append(load.render())
        initWords()
        initialised = true
    }

    fun initWords() {
        sendAjax(base!! + "/symbolon/words", "GET") {
            req ->
            var t = req.responseText
            var ready = req.readyState
            if(ready == 4 as Short) {
                val parsed = JSON.parse<Array<Word>>(t)


                val tm = WordTableModel()
                parsed.each { tm.content.add(it) }
                val table = TableRenderer<Word>(tm, "table")
                table.renderers.put("id", object : CellRenderer<Word> {
                    override fun render(t: Word, v: Any?, row: Int, col: String): FlowContainer {
                        val s = Span()
                        s.a("") {
                            text("${v}")
                            val cb = object : Callback {
                                var w : Word = t
                                override fun callback(event: DOMEvent) {
                                    val s = Span()
                                    s.text(w.name)
                                    jq("div#detail").html(s.render())
                                }
                            }
                            action(cb)
                        }

                        return s
                    }
                })
                val div = Div("content")
                root = div
                val top = Div("top")
                top.select("interests") {
                    option("","-1") {
                        label("NONE")
                        text("NONE")
                    }
                    option("NEW","new") {
                        label("NEW")
                        text("NEW")
                    }
                    val cb = object : Callback {
                        override fun callback(event: DOMEvent) {
                            val s = Span()
                            val sel = jq("#${event.target.id} option:selected")
                            s.text(sel.html())
                            jq("div#detail").html(s.render())
                        }
                    }
                    change(cb)
                }

                div.appendFlow(top)

                val tdiv = Div("container-table")
                div.appendFlow(tdiv)
                val ddiv = Div("detail")
                div.appendFlow(ddiv)
                table.appendTo(tdiv, tm, table)
                fullRender()

            }

        }
    }

    fun fetchWord(id : Long) {
        if(!words.containsKey(id))
            sendAjax(base + "/symbolon/words/word?id=${id}", "GET") {
                req ->
                var t = req.responseText
                var ready = req.readyState
                if(ready == 4 as Short) {
                    //jq("input#out").`val`(t)
                    //val parsed = JSON.parse<Word>(t)

                }
            }
    }



    fun fullRender() {
        jq(rootSelector).html(root.render())
    }
}


public object SESSION : Session()


fun main(args: Array<String>) {

    jq {
        jq("body").on("click", "a.action") {
            event -> SESSION.actionHolder.trigger(event)
        }
        jq("body").on("change", "select.action") {
            event -> SESSION.actionHolder.trigger(event)
        }
        val title = jq("html head title")
        title.text("Words")
        jq("div#uri").text(window.document.baseURI)

        var idx = window.document.baseURI.lastIndexOf("/")
        SESSION.base = window.document.baseURI.substring(0, idx)
        SESSION.session_init()
    }
}






fun sendAjax(url : String, method : String , cb: (req : XMLHttpRequest) -> Unit) {
    var req = XMLHttpRequest()
    req.onreadystatechange = {
        cb(req)
    }
    req.open(method, url)
    req.send()
}


fun<T> Array<T>.each(it:(T)->Unit) {
    for(e in this)  it(e)
}

fun mytable(json : Array<Definition>) :Unit {
    val table : Table = Table("Table")

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
    val name : String = js.noImpl
    val nick : String = js.noImpl
}


