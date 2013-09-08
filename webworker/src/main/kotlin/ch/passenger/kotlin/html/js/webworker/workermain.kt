package ch.passenger.kotlin.html.js.webworker

import js.dom.html.HTMLElement
import ch.passenger.kotlin.html.js.worker.WebWorker
import ch.passenger.kotlin.html.js.binding.DOMEvent
import ch.passenger.kotlin.html.js.worker.WorkerEchoReqRespFactory
import ch.passenger.kotlin.html.js.worker.WorkerReqRespFactory
import ch.passenger.kotlin.html.js.worker.WorkerEchoService
import java.util.HashMap
import ch.passenger.kotlin.html.js.worker.WorkerService


native val self : WebWorker? = js.noImpl

/**
 * Created by sdju on 20.08.13.
 */
fun main(args:Array<String>) {
    services.put("echo", WorkerEchoService())
    init()
}

fun init() {
    val cb : (e:DOMEvent) -> Unit = {
        (e:DOMEvent) ->
        if(e.data is String) {
            self?.postMessage("WW: got ${e.data}")
        }
        else {
            self?.postMessage("all is Json and Json is all")
            self?.postMessage(e?.data?:"worker: event.data was null")
        }
    };
    self?.addEventListener("message", cb, false)
    self?.postMessage("Worker started")

}

val services : MutableMap<String,WorkerService> = HashMap()
