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
    native var debugging : Boolean?
}

public abstract class WorkerRequest(val service:String, val action:String, val client:String) {
    fun toJson() : Json {
        val js = JSON.parse<Json>("{}")
        js.set("type", "request")
        js.set("service", service)
        js.set("action", action)
        js.set("client", client)
        val detail = JSON.parse<Json>("{}")
        jsonDetails(detail)
        js.set("detail", detail)
        return js
    }

    protected abstract fun jsonDetails(detail:Json)

    protected fun createJson() : Json = JSON.parse<Json>("{}")
}

public abstract class WorkerResponse(val service:String, val action:String, val client:String, val success:Boolean=true, val error:Json?=null) {
    fun toJson() : Json {
        val js = JSON.parse<Json>("{}")
        js.set("type", "response")
        js.set("service", service)
        js.set("action", action)
        js.set("client", client)
        js.set("success", success)
        val detail = JSON.parse<Json>("{}")
        if(success)
        jsonDetails(detail)
        else {
            js.set("error", error)
        }
        js.set("detail", detail)
        return js
    }

    protected abstract fun jsonDetails(detail:Json)

    protected fun createJson() : Json = JSON.parse<Json>("{}")
}

public abstract class WorkerReqRespFactory(val service:String, val worker:WebWorker) {
    public fun req(json:Json) : WorkerRequest? {
        val t = json.get("type") as String?
        if(t==null || t!="request") return null
        val s = json.get("service") as String
        if(s!=service) return null
        return resolvereq(json.get("action") as String, json.get("client") as String, json.get("detail") as Json)
    }

    public fun resp(json:Json) : WorkerResponse? {
        val t = json.get("type") as String?
        if(t==null || t!="response") return null
        val s = json.get("service") as String
        if(s!=service) return null
        return resolveresp(json.get("action") as String, json.get("client") as String, json.get("detail") as Json)
    }

    protected abstract fun resolvereq(action:String, client:String, detail:Json) : WorkerRequest?
    protected abstract fun resolveresp(action:String, client:String, detail:Json) : WorkerResponse?
}

class WorkerEchoRequest(val echo:String, client:String) : WorkerRequest("echo", "echo", client) {
    override fun jsonDetails(detail: Json) {
        detail.set("echo", echo)
    }
}

class WorkerDoubleEchoRequest(val echo:String, client:String) : WorkerRequest("echo", "double", client) {
    override fun jsonDetails(detail: Json) {
        detail.set("echo", echo)
    }
}

class WorkerEchoResponse(val echo:String, client:String) : WorkerResponse("echo", "echo", client) {
    override fun jsonDetails(detail: Json) {
        detail.set("echo", echo)
    }
}

class WorkerEchoReqRespFactory(worker:WebWorker) : WorkerReqRespFactory("echo", worker) {
    override fun resolvereq(action: String, client: String, detail: Json): WorkerRequest? {
        val echo = detail.get("echo") as String
        when(action) {
            "echo" -> return WorkerEchoRequest(echo, client)
            "double" -> return WorkerDoubleEchoRequest(echo, client)
            else -> return null
        }
    }
    override fun resolveresp(action: String, client: String, detail: Json): WorkerResponse? {
        val echo = detail.get("echo") as String
        when(action) {
            "echo" -> return WorkerEchoResponse(echo, client)
            else -> return null
        }
    }
}

abstract class WorkerService(val name:String, val worker:WebWorker) {
    abstract val factory : WorkerReqRespFactory
    abstract fun invoke(req:WorkerRequest) : WorkerResponse?
}

class WorkerEchoService(worker:WebWorker) : WorkerService("echo", worker) {
    override val factory: WorkerReqRespFactory = WorkerEchoReqRespFactory(worker)
    override fun invoke(req: WorkerRequest): WorkerResponse? {
        when(req) {
            is WorkerEchoRequest -> return echo(req)
            is WorkerDoubleEchoRequest -> return double(req)
            else -> return null
        }
    }
    fun echo(req:WorkerEchoRequest): WorkerEchoResponse {
        return WorkerEchoResponse(req.echo, req.client)
    }

    fun double(req:WorkerDoubleEchoRequest): WorkerEchoResponse {
        return WorkerEchoResponse(req.echo+req.echo, req.client)
    }
}