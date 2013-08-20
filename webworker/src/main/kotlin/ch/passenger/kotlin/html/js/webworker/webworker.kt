package ch.passenger.kotlin.html.js.webworker

import js.dom.html.HTMLElement
import js.debug.console

public native trait DOMEvent {
    public val target : HTMLElement
    public var data : Any?

    fun targetId() {
        target.id
    }

    fun preventDefault() = js.noImpl
}

native trait Worker {
    native fun postMessage(msg:Any) = js.noImpl
    native fun terminate() = js.noImpl
    native var onmessage : (e:DOMEvent) -> Unit
    native var onerror : (e:DOMEvent) -> Unit
    native fun addEventListener(kind:String, cb: (e:DOMEvent)->Unit, fl:Boolean) = js.noImpl
}


native val self : Worker? = js.noImpl

/**
 * Created by sdju on 20.08.13.
 */
fun main(args:Array<String>) {
    init()
}

fun init() {
    val cb : (e:DOMEvent) -> Unit = {
        (e:DOMEvent) ->
        self?.postMessage("WW: got ${e.data}")
    };
    self?.addEventListener("message", cb, false)
    self?.postMessage("Worker started")
}