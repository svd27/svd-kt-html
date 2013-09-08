package ch.passenger.kotlin.html.js.webworker

import js.dom.html.HTMLElement
import ch.passenger.kotlin.html.js.worker.WebWorker
import ch.passenger.kotlin.html.js.binding.DOMEvent
import ch.passenger.kotlin.html.js.worker.WorkerEchoReqRespFactory
import ch.passenger.kotlin.html.js.worker.WorkerReqRespFactory
import ch.passenger.kotlin.html.js.worker.WorkerEchoService
import java.util.HashMap
import ch.passenger.kotlin.html.js.worker.WorkerService
import ch.passenger.kotlin.html.js.each


native val self: WebWorker? = js.noImpl


/**
 * Created by sdju on 20.08.13.
 */
fun main(args: Array<String>) {
    services.put("echo", WorkerEchoService())
    init()
}

fun debug(s:String) {
    if(self?.debugging?:false)
        self?.postMessage(s)
}

fun init() {
    val cb: (e: DOMEvent) -> Unit = {
        (e: DOMEvent) ->
        if(e.data is String) {
            debug("WW: got ${e.data}")
        }
        else {
            self?.postMessage("all is Json and Json is all: stringify")
            self?.postMessage(JSON.stringify(e?.data?:e))
            if(e.data != null) {
                val js = e.data as Json
                for(s in services.values()) {
                    debug("try service ${s.name}")
                    val req = s.factory.req(js)
                    if(req != null) {
                        debug("req: ${JSON.stringify(req)}")
                        val resp = s.invoke(req)
                        if(resp != null) {
                            debug("resp: ${JSON.stringify(resp)}")
                            self?.postMessage(resp.toJson())
                        }
                    }
                }
            }
        }
    };
    self?.addEventListener("message", cb, false)
    self?.postMessage("Worker started")

}

val services: MutableMap<String, WorkerService> = HashMap()
