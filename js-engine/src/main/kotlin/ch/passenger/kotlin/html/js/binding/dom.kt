package ch.passenger.kotlin.html.js.binding

import js.dom.core.Node
import js.dom.core.NodeList

/**
 * Created by Duric on 18.08.13.
 */
native trait MutationObserverInit  {
    native var childList : Boolean?
    native var attributes : Boolean?
    native var characterData : Boolean?
    native var subtree : Boolean?
    native var attributeOldValue : Boolean?
    native var characterDataOldValue : Boolean?
    native var attributeFilter : Boolean?
}

native trait MutationRecord {
    native val `type` : String
    native val target : Node
    native val addedNodes : NodeList
    native val removedNodes : NodeList
}

native class MutationObserver(cb:(mutations:Array<MutationRecord>)->Unit) {
    native fun observe(target:Node, options:MutationObserverInit) = js.noImpl
    native fun disconnect() = js.noImpl
    native fun takeRecords() : Array<MutationRecord> = js.noImpl

}

public fun NodeList.each(cb:(n:Node)->Unit) {
    val l = this.length.toInt()
    for(i in 0..(l -1)) cb(item(i)!!)
}