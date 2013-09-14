package ch.passenger.kotlin.html.js.logger

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
import java.util.ArrayList


class LogObserver(val client: String, val app: String, val worker: WebWorker) : AbstractObserver<LogEntry>() {
    override fun added(t: LogEntry) {
        val r = LogResponse(app, t, client)
        worker.postMessage(r.toJson())
    }
}

class AppenderFacade(name: String) : Appender(name) {
    override fun write(entry: LogEntry) {
        throw UnsupportedOperationException()
    }
}

class LoggerManagerProxy(val client: String, val worker: WebWorker) : LoggerManager {
    private val appenders: MutableMap<String, AppenderFacade> = HashMap();
    override val TAGS: MutableMap<String, Logger> = HashMap()

    override public val observeLoggers: Observable<Logger> = object : Observable<Logger> {
        protected override val observers: MutableSet<Observer<Logger>> = HashSet()
    }

    override public val observeAppenders: Observable<String> = object : Observable<String> {
        protected override val observers: MutableSet<Observer<String>> = HashSet()
    }

    override fun addAppender(name: String, levels: Array<String>) {
        val req = CreateAppenderRequest(name, levels, client)
        worker.postMessage(req.toJson())
    }

    override fun removeAppender(name: String) {
        worker.postMessage(RemoveAppenderRequest(name, client))
        appenders.remove(name)
    }
    override fun createAppender(name: String, levels: Array<String>): Appender {
        worker.postMessage(CreateAppenderRequest(name, levels, client))
        return AppenderFacade(name)
    }

    override fun appender(name: String): Appender? {
        return appenders.get(name)
    }
    override fun appenders(): Iterable<String> {
        return appenders.keySet()
    }
    override fun appenders(tag: String): Iterable<String> {
        return logger(tag).appenders()
    }
    override fun levels(an: String): Iterable<String> {
        return appender(an)?.levels?:ArrayList()
    }
    override fun levels(an: String, levels: Iterable<String>) {

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
    override fun create(tag: String): Logger = LoggerProxy(worker, tag)
}


class LoggerProxy(val worker: WebWorker, override val tag: String) : Logger {
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
            if(it != null) sb.append(it)
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

class RemoteAppender(val worker: WebWorker, name: String, maxSize: Int = 999999, format: LogFormatter = DefaultLogFormatter()) :
BufferedAppender(name:String, maxSize:Int, format:LogFormatter) {

}

fun wlog(msg: String, w: WebWorker) {
    val js = JSON.parse<Json>("{}")
    js.set("log", msg)
    w.postMessage(js)
}

fun jsReq(service: String, detail: Json): Json {
    val js = JSON.parse<Json>("{}")
    js.set("service", service)
    js.set("detail", detail)
    return js
}

fun jsDetail(action: String): Json {
    val js = JSON.parse<Json>("{}")
    js.set("action", action)
    return js
}



abstract class LoggingRequest(action: String, client: String) : WorkerRequest("logging", action, client)
abstract class LoggingResponse(action: String, client: String, success: Boolean = true, error: Json? = null) : WorkerResponse("logging", action, client, success, error)
abstract class LoggerLoggingRequest(val tag: String, action: String, client: String) : LoggingRequest(action, client) {

    override fun jsonDetails(detail: Json) {
        detail.set("tag", tag)
        loggerDetails(detail)
    }

    abstract fun loggerDetails(detail: Json)
}
abstract class LoggerLoggingResponse(val tag: String, action: String, client: String, success: Boolean = true, error: Json? = null) : LoggingResponse(action, client, success, error) {
    override fun jsonDetails(detail: Json) {
        detail.set("tag", tag)
        loggerDetails(detail)
    }

    abstract fun loggerDetails(detail: Json)
}

abstract class AppenderLoggingRequest(val appender: String, action: String, client: String) : LoggingRequest(action, client) {

    override fun jsonDetails(detail: Json) {
        detail.set("appender", appender)
        appenderDetails(detail)
    }

    abstract fun appenderDetails(detail: Json)
}
abstract class AppenderLoggingResponse(val appender: String, action: String, client: String, success: Boolean = true, error: Json? = null) : LoggingResponse(action, client, success, error) {
    override fun jsonDetails(detail: Json) {
        detail.set("appender", appender)
        appenderDetails(detail)
    }

    abstract fun appenderDetails(detail: Json)
}

class RequestLogger(tag: String, client: String) : LoggerLoggingRequest(tag, "logger", client) {
    override fun loggerDetails(detail: Json) {

    }
}

class LoggerResponse(tag: String, val appenders: Array<String>, client: String, success: Boolean = true, error: Json? = null) :
LoggerLoggingResponse(tag, "logger", client, success, error) {

    override fun loggerDetails(detail: Json) {
        detail.set("appenders", appenders)
    }
}

class CreateAppenderRequest(appender: String, val levels: Array<String>, client: String) : AppenderLoggingRequest(appender, "create-appender", client) {

    override fun appenderDetails(detail: Json) {
        detail.set("levels", levels)
    }
}

class RemoveAppenderRequest(appender: String, client: String) : AppenderLoggingRequest(appender, "remove-appender", client) {

    override fun appenderDetails(detail: Json) {

    }
}

class AppenderLevelsRequest(appender: String, val levels: Array<String>, client: String) : AppenderLoggingRequest(appender, "appender-levels", client) {

    override fun appenderDetails(detail: Json) {
        detail.set("levels", levels)
    }
}


class AppenderResponse(appender: String, val levels: Array<String>, val cfgs: Array<AppenderConfig>, client: String, success: Boolean = true, error: Json? = null) :
AppenderLoggingResponse(appender, "appender", client, success, error) {

    override fun appenderDetails(detail: Json) {
        detail.set("levels", JSON.stringify(levels))
        val ja = Array<Json>(cfgs.size) {
            val js = createJson()
            js.set("tag", cfgs.get(it).tag)
            js.set("levels", JSON.stringify(cfgs.get(it).levels))
            js
        }
        detail.set("cfgs", ja)
    }
}

class LogRequest(tag: String, val level: String, val content: String, client: String) : LoggerLoggingRequest(tag, "log", client) {
    override fun loggerDetails(detail: Json) {
        detail.set("level", level)
        detail.set("content", content)
    }
}

class LogResponse(appender: String, val log: LogEntry, client: String) : AppenderLoggingResponse(appender, "log", client) {

    override fun appenderDetails(detail: Json) {
        val le = createJson()
        le.set("tag", log.tag)
        le.set("level", log.level)
        le.set("content", log.content)
        le.set("date", log.date.getTime())
        detail.set("logentry", le)
    }
}

class TrackAppenderRequest(appender: String, client: String) : AppenderLoggingRequest(appender, "track", client) {
    override fun appenderDetails(detail: Json) {

    }
}

class TrackAppenderResponse(appender: String, client: String, success: Boolean = true, error: Json? = null) :
AppenderLoggingResponse(appender, "track", client, success, error) {
    override fun appenderDetails(detail: Json) {

    }
}

class LoggerAddAppenderRequest(tag: String, val appender: String, client: String) : LoggerLoggingRequest(tag, "logger-add-appender", client) {

    override fun loggerDetails(detail: Json) {
        detail.set("appender", appender)
    }
}

class LoggerAddAppenderResponse(tag: String, val appender: String, client: String, success: Boolean = true, error: Json? = null) :
LoggerLoggingResponse(tag, "logger-add-appender", client, success, error) {

    override fun loggerDetails(detail: Json) {
        detail.set("appender", appender)
    }
}

class LoggerRemoveAppenderRequest(tag: String, val appender: String, client: String) : LoggerLoggingRequest(tag, "logger-remove-appender", client) {
    override fun loggerDetails(detail: Json) {
        detail.set("appender", appender)
    }
}

class LoggingReqRespFactory(worker:WebWorker) : WorkerReqRespFactory("logging", worker) {

    override fun resolvereq(action: String, client: String, detail: Json): WorkerRequest? {
        when(action) {
            "logger" -> return RequestLogger(detail.get("tag") as String, client)
            "create-appender" -> {

                return CreateAppenderRequest(detail.get("appender") as String,
                        detail.get("levels") as Array<String>, client)
            }
            "remove-appender" -> return RemoveAppenderRequest(detail.get("appender") as String, client)
            "appender-levels" -> return AppenderLevelsRequest(detail.getString("appender"), detail.get("levels") as Array<String>, client)
            "logger-add-appender" -> {
                return LoggerAddAppenderRequest(detail.getString("tag"), detail.getString("appender"), client)
            }
            "logger-remove-appender" -> {
                return LoggerRemoveAppenderRequest(detail.getString("tag"), detail.getString("appender"), client)
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
            "logger-add-appender" -> {
                return LoggerAddAppenderResponse(detail.getString("tag"), detail.getString("appender"), client)
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


class LoggerService(val mgr: LocalLoggerManager = LocalLoggerManager(), worker:WebWorker) : WorkerService("logging", worker), LoggerManager by mgr {
    val trackers : MutableMap<String,RemoteTracker> = HashMap()
    override val factory: WorkerReqRespFactory = LoggingReqRespFactory(worker)
    override fun invoke(req: WorkerRequest): WorkerResponse? {
        when(req) {
            is RequestLogger -> {
                val logger = mgr.logger(req.tag)
                val apparr = Array<String>(logger.appenders().size()) {
                    logger.appenders().get(it)
                }
                return LoggerResponse(req.tag, apparr, req.client)
            }
            is LogRequest -> {
                val logger = mgr.logger(req.tag)
                logger.log(req.level, req.content)
                return null
            }
            is CreateAppenderRequest -> {
                mgr.addAppender(req.appender, req.levels)
                val appender = mgr.appender(req.appender)
                worker.postMessage("created ${appender?.name} js: " + JSON.stringify(appender?:"my bad"))
                val lvls = ArrayList<String>(appender?.levels?.size()?:0)
                lvls.addAll(appender?.levels?:ArrayList())
                val lvlarr = Array<String>(lvls.size()) {
                    lvls.get(it)
                }
                worker.postMessage("cfgs: ${appender?.cfgs?.size()} ${appender?.cfgs}")
                worker.postMessage("lvlarr: ${lvlarr} ${lvlarr.size}")
                val cl = ArrayList<AppenderConfig>(appender?.cfgs?.size()?:0)
                appender?.cfgs?.values()?.each {
                    cl.add(it)
                }

                val cfgarr = Array<AppenderConfig>(appender?.cfgs?.size()?:0) {
                    worker.postMessage("cfg $it: ${appender!!.cfgs}")
                    val cfg = cl.get(it)
                    cfg!!
                }
                worker.postMessage("sending resp")
                return AppenderResponse(appender?.name?:"",
                        lvlarr,
                        cfgarr, req.client)
            }
            is AppenderLevelsRequest -> {
                val appender = mgr.appender(req.appender)
                if(appender != null) {
                    req.levels.each { appender.addLevel(it) }
                }
            }
            is TrackAppenderRequest -> {
                val appender = mgr.appender(req.appender)
                if(appender != null) {
                    if(!trackers.containsKey(req.client))
                        trackers.put(req.client, RemoteTracker(req.client, worker, req.appender))
                    appender.addObserver(trackers.get(req.client)!!)
                }
            }
            else -> return null
        }
        return null
    }
}

class RemoteTracker(val client:String, val worker : WebWorker, val appender:String) : AbstractObserver<LogEntry>() {
    override fun added(t: LogEntry) {
        worker.postMessage(LogResponse(appender, t, client).toJson())
    }
}