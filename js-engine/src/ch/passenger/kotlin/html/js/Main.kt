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



fun main(args: Array<String>) {
    var session : Session = SESSION
    jq {
        /*
        jq("body").on("click", "a.action") {
            event -> session.actionHolder.trigger(event)
        }
        */
        val title = jq("html head title")
        title.text("Words")
        jq("div#uri").text(window.document.baseURI)

        var idx = window.document.baseURI.lastIndexOf("/")
        session.base = window.document.baseURI.substring(0, idx)
        session.init()
        /*
        jq("div#base").text(Session.base!!)


        sendAjax(Session.base!! + "/symbolon/words", "GET") {
            req ->
            var t = req.responseText
            var ready = req.readyState
            if(ready == 4 as Short) {
                jq("input#out").`val`(t)
                val parsed = JSON.parse<Array<Word>>(t)

                jq("div#json").text(t)

                wordTable("div#table", parsed)
            }
        }
        */
    }


}

public object SESSION : Session()

open public class Session {
    var base : String? = null
    val words = HashMap<Long,Word>()
    var root : FlowContent = Div()
    var rootSelector : String = "body"
    var initialised = false
    val actionHolder = ActionHolder()

    fun init() {
        if(initialised) return
        root = Div()
        root.text("Loading...")
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
                val table = TableRenderer<Word>("table", tm, "table")
                table.append(root)
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



fun sendAjax(url : String, method : String , cb: (req : XMLHttpRequest) -> Unit) {
    var req = XMLHttpRequest()
    req.onreadystatechange = {
        cb(req)
    }
    req.open(method, url)
    req.send()
}

fun wordTable(sel : String, wl: Array<Word>) {
    val dict : MutableMap<Long,Word> = HashMap()

    for(w in wl) dict.put(w.id, w)

    val table : Table = Table()

    table.caption {
        text("Table")
    }

    table.body {
        for(w in wl) {
            tr {
                if(w.kind==null) {
                    atts {
                        att("class", "typeWord")
                    }
                }

                td {
                    a("") {
                        class CB : Callback {
                            override fun callback(event : DOMEvent) {
                                jq("div#details").text(w.name)
                            }
                        }
                        val cb = CB()
                        action(cb)
                        text(w.name)
                    }
                }
                td {
                    text(w.id.toString())
                }
                td {
                    if(w.kind!=null && dict[w.kind]!=null) text(dict[w.kind]!!.name)
                }
            }
        }
    }

    table.head {
        tr {
            td {
                text("Name")
            }

            td {
                text("ID")
            }

            td {
                text("Kind")
            }
        }
    }


    val html = table.toString()

    jq(sel).append(html)
}

fun<T> Array<T>.each(it:(T)->Unit) {
    for(e in this)  it(e)
}

fun mytable(json : Array<Definition>) :Unit {
    val table : Table = Table()

    table.caption {
        text("Table")
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


