package ch.passenger.kotlin.html.js.webworker

import js.dom.html.HTMLElement
import ch.passenger.kotlin.html.js.worker.WebWorker
import ch.passenger.kotlin.html.js.binding.DOMEvent


native val self : WebWorker? = js.noImpl

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