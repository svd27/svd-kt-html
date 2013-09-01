package ch.passenger.kotlin.html.js.worker

import ch.passenger.kotlin.html.js.binding.DOMEvent


/**
 * Created by sdju on 20.08.13.
 */
native class Worker(script:String) {
    native fun postMessage(msg:Any) = js.noImpl
    native fun terminate() = js.noImpl
    native var onmessage : (e:DOMEvent) -> Unit = js.noImpl
    native var onerror : (e:DOMEvent) -> Unit = js.noImpl
    native fun addEventListener(kind:String, cb: (e:DOMEvent)->Unit, fl:Boolean) = js.noImpl
}

native("Worker") trait WebWorker {
    native fun postMessage(msg:Any)
    native fun terminate()
    native var onmessage : (e:DOMEvent) -> Unit
    native var onerror : (e:DOMEvent) -> Unit
    native fun addEventListener(kind:String, cb: (e:DOMEvent)->Unit, fl:Boolean)
    native var attributes : MutableMap<String,Any?>?
}


