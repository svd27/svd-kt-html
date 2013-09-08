import ch.passenger.kotlin.html.js.worker.WorkerService
import ch.passenger.kotlin.html.js.worker.WebWorker
import ch.passenger.kotlin.html.js.binding.EventTypes
import ch.passenger.kotlin.html.js.logger.LocalLoggerManager
import ch.passenger.kotlin.html.js.binding.DOMEvent
import ch.passenger.kotlin.html.js.model.AbstractObserver
import ch.passenger.kotlin.html.js.each
import ch.passenger.kotlin.html.js.logger.LogEntry
import ch.passenger.kotlin.html.js.logger.LoggerManager
import ch.passenger.kotlin.html.js.logger.Logger
import ch.passenger.kotlin.html.js.logger.Appender
import ch.passenger.kotlin.html.js.logger.BufferedAppender
import ch.passenger.kotlin.html.js.logger.LogFormatter
import ch.passenger.kotlin.html.js.model.Observer
import java.util.HashMap
import ch.passenger.kotlin.html.js.model.Observable
import java.util.StringBuilder
import java.util.HashSet
import ch.passenger.kotlin.html.js.logger.DefaultLogFormatter
import ch.passenger.kotlin.html.js.worker.WorkerReqRespFactory
import ch.passenger.kotlin.html.js.worker.WorkerRequest
import ch.passenger.kotlin.html.js.worker.WorkerResponse
import ch.passenger.kotlin.html.js.logger.AppenderConfig
import ch.passenger.kotlin.html.js.util.getString
import ch.passenger.kotlin.html.js.util.getLong

class RemoteLoggerManager(val worker: WebWorker) : LocalLoggerManager() {
    {
        val cb = {
            (evt: DOMEvent) ->
            val ed = evt.data
            if(ed != null) {
                val data: Json = ed as Json
                val service = data.get("service")
                if(service != null && (service is String) && service == "logging") {
                    evt.preventDefault()
                    val detail = data.get("detail")
                    if(detail != null) {
                        val d = detail as Json
                        val a = d.get("action") as String?
                        if(a!=null)
                            dispatch(a, detail)
                        else error("unknown action $a")
                    }
                }
            }
        }
        worker.addEventListener(EventTypes.message.name(), cb, false)
    }

    fun error(msg:String) {
        val res = JSON.parse<Json>("{}")
        res.set("error", msg)
        worker.postMessage(res)
    }

    fun dispatch(action: String, detail: Json) {
        val resp = JSON.parse<Json>("{}")
        when(action) {
            "logger" -> {
                val tag = resp.get("tag") as String
                logger(tag)
                val res = JSON.parse<Json>("{}")
                res.set("result", "ok")
                res.set("logger", logger2json(logger(tag)))
                worker.postMessage(res)
            }
            "create-appender" -> {
                val name = resp.get("name") as String
                val levels = resp.get("levels") as List<String>
                var appender = appenders.get(name)
                if(appender==null)
                    addAppender(name, levels)
                appender = appenders.get(name)!!
                val res = JSON.parse<Json>("{}")
                res.set("result", "ok")
                res.set("appender", appender2json(appender!!))
                worker.postMessage(res)
            }
            "appender-remove-level" -> {
                val name = resp.get("name") as String
                val levels = resp.get("levels") as List<String>
                var appender = appenders.get(name)
                if(appender==null)
                    addAppender(name, levels)
                appender = appenders.get(name)!!
                val res = JSON.parse<Json>("{}")
                res.set("result", "ok")
                res.set("appender", appender2json(appender!!))
                worker.postMessage(res)
            }
            "log-add-appender" -> {
                val res = JSON.parse<Json>("{}")
                val tag = detail.get("tag") as String
                val na  = detail.get("appender") as String
                val app = appenders.get(na)
                if(app!=null && TAGS.containsKey(tag)) {
                    logger(tag).addAppender(app)
                    res.set("result", "ok")
                    res.set("logger", logger2json(logger(tag)))
                }
                resp.set("error", "not found")
                resp.set("detail", detail)

                worker.postMessage(res)
            }
            "log-remove-appender" -> {
                val res = JSON.parse<Json>("{}")
                val tag = detail.get("tag") as String
                val na  = detail.get("appender") as String
                val app = appenders.get(na)
                if(app!=null && TAGS.containsKey(tag)) {
                    logger(tag).removeAppender(app)
                    res.set("result", "ok")
                    res.set("logger", logger2json(logger(tag)))
                }
                resp.set("error", "not found")
                resp.set("detail", detail)

                worker.postMessage(res)
            }
            "track" -> {
                val na  = detail.get("appender") as String
                val client = detail.get("client") as String

                track(na, LogObserver(client, na, worker))
            }
            "untrack" -> {
                val na  = detail.get("appender") as String
                val client = detail.get("client") as String

                val app = appenders.get(na)
                if(app!=null) {
                    //TODO: dont trust equals
                    app.observers().each {
                        if(it is LogObserver && it.client==client) {
                            val js = JSON.parse<Json>("{}")
                            js.set("log", "untrack $client on $na")
                            worker.postMessage(js)
                            untrack(na, it)
                        }
                    }
                }

            }
            "log" -> {
                val tag = detail.get("tag") as String
                val log = logger(tag)
                if(log!=null) {
                    val level = detail.get("content") as String
                    val content = detail.get("content")
                    log.log(level, content)
                }
            }
            else -> {
                resp.set("error", "dont understand")
                resp.set("detail", detail)
                worker.postMessage(resp)
            }
        }
    }

    class LogObserver(val client:String, val app:String, val worker:WebWorker) :  AbstractObserver<LogEntry>() {
        override fun added(t: LogEntry) {
            val msg = JSON.parse<Json>("{}")
            msg.set("client", client)
            msg.set("logentry", t)
            worker.postMessage(msg)
        }
    }

    fun logger2json(l:Logger): Json {
        val js = JSON.parse<Json>("{}")
        js.set("tag", l.tag)
        js.set("appenders", l.appenders().toArray())
        return js
    }

    fun appender2json(a:Appender) : Json {
        val js = JSON.parse<Json>("{}")
        js.set("appender", a.name)
        val la = a.levels.toArray()
        js.set("levels", la)
        return js
    }


    protected override fun createAppender(name: String, levels: Iterable<String>): Appender {
        val rap = RemoteAppender(worker, name)
        levels.each { rap.addLevel(it) }
        return rap
    }
}

class LoggerManagerProxy(val worker:WebWorker) : LoggerManager {
    override val TAGS: MutableMap<String, Logger> = HashMap()
    override public val observeLoggers : Observable<Logger> = object : Observable<Logger> {
        protected override val observers: MutableSet<Observer<Logger>> = HashSet()
    }

    override public val observeAppenders : Observable<String> = object : Observable<String> {
        protected override val observers: MutableSet<Observer<String>> = HashSet()
    }

    override fun addAppender(name: String, levels: Iterable<String>) {
        val detail = jsDetail("create-appender")
        detail.set("name", name)
        val req = jsReq("logging", detail)

        worker.postMessage(req)
    }

    override fun removeAppender(name: String) {
        throw UnsupportedOperationException()
    }
    override fun createAppender(name: String, levels: Iterable<String>): Appender {
        throw UnsupportedOperationException()
    }
    override fun appender(name: String): Appender? {
        throw UnsupportedOperationException()
    }
    override fun appenders(): Iterable<String> {
        throw UnsupportedOperationException()
    }
    override fun appenders(tag: String): Iterable<String> {
        throw UnsupportedOperationException()
    }
    override fun levels(an: String): Iterable<String> {
        throw UnsupportedOperationException()
    }
    override fun levels(an: String, levels: Iterable<String>) {
        throw UnsupportedOperationException()
    }
    override fun cfg(an: String, tag: String, levels: Iterable<String>) {
        throw UnsupportedOperationException()
    }
    override fun track(appender: String, obs: Observer<LogEntry>) {
        throw UnsupportedOperationException()
    }
    override fun untrack(appender: String, obs: Observer<LogEntry>) {
        throw UnsupportedOperationException()
    }
    override fun create(tag: String): Logger {
        throw UnsupportedOperationException()
    }
}


class LoggerProxy(val worker:WebWorker, override val tag: String) : Logger {
    public override fun log(level: String, vararg content: Any?) {
        val js = JSON.parse<Json>("{}")
        js.set("service", "logging")
        val detail = JSON.parse<Json>("{}")
        detail.set("action", "log")
        val jl = JSON.parse<Json>("{}")
        jl.set("tag", tag)
        jl.set("level", level)
        val sb = StringBuilder()
        content.each {
            if(it!=null) sb.append(it)
            else sb.append("null")
        }
        jl.set("content", sb.toString())
        detail.set("log", jl)
        js.set("detail", detail)
        worker.postMessage(jl)
    }
    override fun addAppender(a: Appender) {
        throw UnsupportedOperationException()
    }
    override fun removeAppender(a: Appender) {
        throw UnsupportedOperationException()
    }
    override fun appenders(): List<String> {
        throw UnsupportedOperationException()
    }
    override fun clearAppenders() {
        throw UnsupportedOperationException()
    }
}

class RemoteAppender(val worker:WebWorker, name:String, maxSize:Int=999999, format:LogFormatter=DefaultLogFormatter()) :
BufferedAppender(name:String, maxSize:Int, format:LogFormatter) {

}

fun wlog(msg:String, w:WebWorker) {
    val js = JSON.parse<Json>("{}")
    js.set("log", msg)
    w.postMessage(js)
}

fun jsReq(service:String, detail:Json) : Json {
    val js = JSON.parse<Json>("{}")
    js.set("service", service)
    js.set("detail", detail)
    return js
}

fun jsDetail(action:String) : Json {
    val js = JSON.parse<Json>("{}")
    js.set("action", action)
    return js
}



abstract class LoggingRequest(action:String, client:String) : WorkerRequest("logging", action, client)
abstract class LoggingResponse(action:String, client:String, success:Boolean=true, error:Json?=null) : WorkerResponse("logging", action, client, success, error)
abstract class LoggerLoggingRequest(val tag:String,action:String, client:String) : LoggingRequest(action, client) {

    override fun jsonDetails(detail: Json) {
        detail.set("tag", tag)
        loggerDetails(detail)
    }

    abstract fun loggerDetails(detail:Json)
}
abstract class LoggerLoggingResponse(val tag:String, action:String, client:String, success:Boolean=true, error:Json?=null) : LoggingResponse(action, client, success, error) {
    override fun jsonDetails(detail: Json) {
        detail.set("tag", tag)
        loggerDetails(detail)
    }

    abstract fun loggerDetails(detail:Json)
}

abstract class AppenderLoggingRequest(val appender:String,action:String, client:String) : LoggingRequest(action, client) {

    override fun jsonDetails(detail: Json) {
        detail.set("appender", appender)
        appenderDetails(detail)
    }

    abstract fun appenderDetails(detail:Json)
}
abstract class AppenderLoggingResponse(val appender:String, action:String, client:String, success:Boolean=true, error:Json?=null) : LoggingResponse(action, client, success, error) {
    override fun jsonDetails(detail: Json) {
        detail.set("appender", appender)
        appenderDetails(detail)
    }

    abstract fun appenderDetails(detail:Json)
}

class RequestLogger(tag:String, client:String) : LoggerLoggingRequest(tag, "logger", client) {
    override fun loggerDetails(detail: Json) {

    }
}

class LoggerResponse(tag:String, val appenders:Array<String>, client:String, success:Boolean=true, error:Json?=null) :
        LoggerLoggingResponse(tag, "logger", client, success, error) {

    override fun loggerDetails(detail: Json) {
        detail.set("appenders", appenders)
    }
}

class CreateAppenderRequest(appender:String, val levels:Array<String>, client:String) : AppenderLoggingRequest(appender, "create-appender", client) {

    override fun appenderDetails(detail: Json) {
        detail.set("levels", levels)
    }
}

class AppenderResponse(appender:String, val levels:Array<String>, val cfgs:Array<AppenderConfig>, client:String, success:Boolean=true, error:Json?=null) :
AppenderLoggingResponse(appender, "appender", client, success, error) {

    override fun appenderDetails(detail: Json) {
        detail.set("levels", levels)
        val ja = Array<Json>(cfgs.size) {
            val js = createJson()
            js.set("tag", cfgs.get(it).tag)
            js.set("levels", cfgs.get(it).levels)
            js
        }
        detail.set("cfgs", ja)
    }
}

class LogRequest(tag:String, val level:String, val content:String, client:String) : LoggerLoggingRequest(tag, "log", client) {
    override fun loggerDetails(detail: Json) {
        detail.set("level", level)
        detail.set("content", content)
    }
}

class LogResponse(appender:String, val log:LogEntry, client:String) : AppenderLoggingResponse(appender, "log", client) {

    override fun appenderDetails(detail: Json) {
        val le = createJson()
        le.set("tag", log.tag)
        le.set("level", log.level)
        le.set("content", log.content)
        le.set("date", log.date.getTime())
        detail.set("logentry", le)
    }
}

class TrackAppenderRequest(appender:String, client:String) : AppenderLoggingRequest(appender, "track", client) {
    override fun appenderDetails(detail: Json) {

    }
}

class TrackAppenderResponse(appender:String, client:String, success:Boolean=true, error:Json?=null) :
AppenderLoggingResponse(appender, "track", client, success, error) {
    override fun appenderDetails(detail: Json) {

    }
}

class AddAppenderRequest(tag:String, val appender:String, client:String) : LoggerLoggingRequest(tag, "add-appender", client) {

    override fun loggerDetails(detail: Json) {
        detail.set("appender", appender)
    }
}

class AddAppenderResponse(tag:String, val appender:String, client:String, success:Boolean=true, error:Json?=null) :
LoggerLoggingResponse(tag, "add-appender", client, success, error) {

    override fun loggerDetails(detail: Json) {
        detail.set("appender", appender)
    }
}


class LoggingReqRespFactory : WorkerReqRespFactory("logging") {

    override fun resolvereq(action: String, client: String, detail: Json): WorkerRequest? {
        when(action) {
            "logger" -> return RequestLogger(detail.get("tag") as String, client)
            "create-appender" -> return CreateAppenderRequest(detail.get("appender") as String,
                    detail.get("levels") as Array<String>, client)
            "add-appender" -> {
                return AddAppenderRequest(detail.getString("tag"), detail.getString("apeender"), client)
            }
            "log" -> {
                val tag = detail.getString("tag")
                val level = detail.getString("level")
                val content = detail.getString("content")
                return LogRequest(tag, level, content, client)
            }
            else -> return null
        }
    }
    override fun resolveresp(action: String, client: String, detail: Json): WorkerResponse? {
        when(action) {
            "logger" -> return LoggerResponse(detail.get("tag") as String,
                    detail.get("appenders") as Array<String>, client)
            "appender" -> {
                val app = detail.get("appender") as String
                val levels = detail.get("levels") as Array<String>
                val cfgs = detail.get("cfgs") as Array<Json>

                val aca = Array<AppenderConfig>(cfgs.size) {
                    val js = cfgs.get(it)
                    val ac = AppenderConfig(js.get("tag") as String)
                    val la = detail.get("levels") as Array<String>
                    la.each {
                        ac.addLevel(it)
                    }

                    ac
                }
                return AppenderResponse(app, levels, aca, client)
            }
            "add-appender" -> {
                return AddAppenderResponse(detail.getString("tag"), detail.getString("appender"), client)
            }
            "log" -> {
                val app = detail.getString("appender")
                val le = detail.get("logentry") as Json
                val tag = le.getString("tag")
                val content = le.getString("content")
                val level = le.getString("level")
                val ts = le.getLong("date")
                val date = ch.passenger.kotlin.html.js.binding.Date(ts)
                val log = LogEntry(tag, level, content, date)
                return LogResponse(app, log, client)
            }
            else -> return null
        }
    }
}


class LoggerService(val mgr:LocalLoggerManager=LocalLoggerManager()) : WorkerService("logging"), LoggerManager by mgr {

    override val factory: WorkerReqRespFactory = LoggingReqRespFactory()
    override fun invoke(req: WorkerRequest): WorkerResponse? {
        when(req) {
            is RequestLogger -> {
                val logger = logger(req.tag)
                return LoggerResponse(req.tag, logger.appenders().toArray() as Array<String>, req.client)
            }
            is LogRequest -> {
                val logger = logger(req.tag)
                logger.log(req.level, req.content)
                return null
            }
            else -> return null
        }

    }
}