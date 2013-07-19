package ch.passenger.kotlin.html.js.binding

import ch.passenger.kotlin.html.js.html.DOMEvent

/**
 * Created by sdju on 19.07.13.
 */
public native class WebSocket(val url : String) {
    val readyState : Int = js.noImpl
    var onclose : (e : DOMEvent) -> Unit = js.noImpl
    var onerror : (e : DOMEvent) -> Unit = js.noImpl
    var onmessage : (e : DOMEvent) -> Unit = js.noImpl
    var onopen : (e : DOMEvent) -> Unit = js.noImpl

    fun close(code : Long?, reason : String?) = js.noImpl
    fun send(msg : String) = js.noImpl
}