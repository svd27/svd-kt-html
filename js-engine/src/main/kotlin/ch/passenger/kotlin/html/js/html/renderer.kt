package ch.passenger.kotlin.html.js.html

import js.dom.core.Document
import js.debug.console
import ch.passenger.kotlin.html.js.binding.each

/**
 * Created by sdju on 19.08.13.
 */
class Renderer(protected val doc:Document) {
    fun render(e:HtmlElement) {
        console.log("render $e:${e.id()} hidden: ${e.hidden} dirty: ${e.dirty}")
        if(!e.hidden && e.dirty) {
            var node = e.node
            if(node ==null)
                node = e.createNode()
            if(node==null) return
            e.refresh()
            e.each {
                render(it)
            }
        }
    }

    fun body(e:HtmlElement) {
        val body = doc.getElementsByTagName("body").item(0)!!
        body.childNodes.each {
            body.removeChild(it)
        }
        val n = if(e.node==null) e.createNode() else e.node
        if(n==null) return
        body.appendChild(n!!)
        render(e)
    }
}