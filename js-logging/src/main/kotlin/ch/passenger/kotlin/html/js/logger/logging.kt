package ch.passenger.kotlin.html.js.logger

import js.debug.console
import java.util.ArrayList
import java.util.StringBuilder
import java.util.HashMap
import java.util.HashSet
import ch.passenger.kotlin.html.js.model.Observable
import ch.passenger.kotlin.html.js.model.Observer
import ch.passenger.kotlin.html.js.binding.Date
import ch.passenger.kotlin.html.js.each
import ch.passenger.kotlin.html.js.binding.EventTypes
import ch.passenger.kotlin.html.js.binding.DOMEvent
import ch.passenger.kotlin.html.js.listOf
import ch.passenger.kotlin.html.js.worker.WebWorker
import ch.passenger.kotlin.html.js.model.AbstractObserver


/**
 * Created by Duric on 25.08.13.
 */

class LogEntry(val tag:String, val level:String, val content:String, val date:Date)

abstract class LogFormatter() {
    abstract fun format(e:LogEntry) : String
}

class DefaultLogFormatter() : LogFormatter() {

    override fun format(e: LogEntry) = "${e.level}::${e.tag}::${e.date.getHours()}:${e.date.getMinutes()}:${e.date.getSeconds()}:${e.date.getMilliseconds()}:: ${e.content}\n"
}

class AppenderConfig(val tag:String) {
    val levels:MutableSet<String> = HashSet()

    fun accept(level:String) : Boolean {
        if(levels.contains("ALL")) return true

        return levels.contains(level)
    }

    fun addLevel(level:String) {
        levels.remove("ALL")
        levels.add(level)
    }

    fun removeLevel(level:String) {
        levels.remove(level)
    }
}

abstract class Appender(val name:String, var format:LogFormatter=DefaultLogFormatter(), val noop:Boolean=false) : Observable<LogEntry> {
    val cfgs : MutableMap<String,AppenderConfig> = HashMap();
    val levels : MutableSet<String> = HashSet();

    {
        val cfg = AppenderConfig("ROOT")
        cfg.levels.add("ALL")
        cfgs.put("ROOT", cfg)
        levels.add("ALL")
    }

    fun addLevel(level:String) {
        levels.remove("ALL")
        levels.add(level)
    }

    fun removeLevel(level:String) {
        levels.remove(level)
    }

    fun cfg(tag:String, cfg:AppenderConfig) {
        cfgs.put(tag, cfg)
    }
    fun cfg(tag:String) : AppenderConfig {
        if(!cfgs.containsKey(tag)) {
            var cfg : AppenderConfig = resolveConfig(tag)
            cfgs.put(tag, cfg)
        }
        console.log("$tag -> cfg:${cfgs.get(tag)?.tag}")

        return cfgs.get(tag)!!
    }

    private fun resolveConfig(tag:String): AppenderConfig {
        if(!cfgs.containsKey(tag)) {
            val idx = tag.lastIndexOf(".")
            if(idx>0) {
                val parent = tag.substring(0, idx)
                return resolveConfig(parent)
            } else {
                return cfgs.get("ROOT")!!
            }
        }

        return cfgs.get(tag)!!
    }

    abstract fun write(entry:LogEntry)

    fun write(level:String, tag:String, content:String) {
        //TODO: add formatters
        val date = Date(Date.now())
        write(LogEntry(tag, level, content, date))
        if(observers.size()>0) {
            fireAdd(LogEntry(tag, level, content, date))
        }
    }

    private fun iWant(level:String) :Boolean {
        return levels.contains("ALL") || levels.contains(level)
    }

    open fun want(tag:String, level:String) : Boolean {
        return iWant(level) && cfg(tag).accept(level)
    }

    open fun currentContent() : List<LogEntry> = ArrayList()

    protected override val observers: MutableSet<Observer<LogEntry>> = HashSet()

    fun observers() : Set<Observer<LogEntry>> = observers
}

class ConsoleAppender(name:String, format:LogFormatter=DefaultLogFormatter()) : Appender(name, format) {
    override fun write(entry: LogEntry) {
        console.log(format.format(entry))
    }
}

class NullAppender() : Appender("/dev/null", DefaultLogFormatter(), true) {
    override fun write(entry: LogEntry) {}

    override fun want(tag: String, level: String): Boolean = false
}

open class BufferedAppender(name:String, val maxSize:Int=999999, format:LogFormatter=DefaultLogFormatter()) : Appender(name, format) {
    val lines : MutableList<LogEntry> = ArrayList()


    override fun write(e:LogEntry) {
        if(lines.size()>maxSize) throw IllegalArgumentException()
        if(lines.size()==maxSize) {
            lines.remove(0)
        }
        lines.add(e)

        write(e)
        writeHook(e)
    }

    open fun writeHook(e:LogEntry) {

    }


    override fun currentContent(): List<LogEntry> = lines
}

trait Logger {
    val tag:String

    public fun log(level:String, vararg content:Any?)

    public fun debug(vararg content:Any?) : Unit = log("DEBUG", content)
    public fun info(vararg content:Any?) : Unit = log("INFO", content)
    public fun warn(vararg content:Any?) : Unit = log("WARN", content)
    public fun error(vararg content:Any?) : Unit = log("ERROR", content)
    public fun fatal(vararg content:Any?) : Unit = log("FATAL", content)

    fun addAppender(a:Appender)

    fun removeAppender(a:Appender)

    fun appenders() : List<String>

    fun clearAppenders()
}



class LocalLogger (override val tag:String) : Logger {
    protected var appenders:MutableList<Appender> = ArrayList()

    override public fun log(level:String, vararg content:Any?) {
        var payload : String? = null
        appenders.each {
            if (payload == null) {
                val sb = StringBuilder()
                content.each {
                    if(it != null)
                        sb.append(it)
                    else sb.append("null")
                }
                payload = sb.toString()
            }
            it.write(level, tag, payload!!)
        }

    }

    override fun addAppender(a: Appender) {
        appenders.add(a)
    }
    override fun removeAppender(a: Appender) {
        appenders.remove(a)
    }
    override fun appenders(): List<String> {
        val res = ArrayList<String>()
        appenders.each { res.add(it.name) }
        return res
    }
    override fun clearAppenders() {
        appenders.clear()
    }
}

trait LoggerManager  {
    protected val TAGS : MutableMap<String,Logger>
    public val observeLoggers : Observable<Logger>
    public val observeAppenders : Observable<String>

    fun logger(tag: String) : Logger {
        if(TAGS.containsKey(tag)) return TAGS.get(tag)!!
        TAGS.put(tag, create(tag))
        return logger(tag)
    }

    fun addAppender(name:String, levels:Array<String>)

    open fun removeAppender(name:String)

    fun createAppender(name:String, levels:Array<String>):Appender
    fun appender(name:String) : Appender?

    fun loggers() : Iterable<String> = TAGS.keySet()
    fun appenders() : Iterable<String>
    fun appenders(tag:String) : Iterable<String>
    fun levels(an:String) : Iterable<String>
    fun levels(an:String, levels : Iterable<String>)
    fun cfg(an:String, tag:String, levels:Iterable<String>)

    fun track(appender:String, obs:Observer<LogEntry>)
    fun untrack(appender:String, obs:Observer<LogEntry>)
    fun content(appender:String) : List<LogEntry> {
        val app = appender(appender)
        if(app!=null) {
            return app.currentContent()
        }
        return ArrayList()
    }

    protected fun create(tag:String) : Logger
}


class Tracker() : AbstractObserver<LogEntry>() {
    val cbs: MutableList<(LogEntry) -> Unit> = ArrayList(1)
    override fun added(entry: LogEntry) {
        cbs.each { it.invoke(entry) }
    }
}

open class LocalLoggerManager() : LoggerManager {
    protected override val TAGS: MutableMap<String, Logger> = HashMap()
    protected val appenders : MutableMap<String,Appender> = HashMap<String,Appender>()
    val DEFAULT : Appender=ConsoleAppender("DEFAULT");

    {
        appenders.put(DEFAULT.name, DEFAULT)
    }

    override public val observeLoggers : Observable<Logger> = object : Observable<Logger> {
        protected override val observers: MutableSet<Observer<Logger>> = HashSet()
    }

    override public val observeAppenders : Observable<String> = object : Observable<String> {
        protected override val observers: MutableSet<Observer<String>> = HashSet()
    }

    override fun addAppender(name: String, levels: Array<String>) {
        appenders.put(name, createAppender(name, levels))
        observeAppenders.fireAdd(name)
    }

    override fun removeAppender(name: String) {
        appenders.remove(name)
        observeAppenders.fireRemove(name)
        observeAppenders.fireDelete(name)
    }

    override open fun createAppender(name: String, levels: Array<String>): Appender {
        val appender = buffer(name)
        levels.each { appender.addLevel(it) }
        return appender
    }


    override fun appender(name: String): Appender? = appenders.get(name)

    override fun appenders(): Iterable<String> {
        return appenders.keySet()
    }
    override fun appenders(tag: String): Iterable<String> {
        if(TAGS.containsKey(tag)) {
            return logger(tag).appenders()
        }
        return ArrayList()
    }

    protected override fun create(tag: String): Logger {
        val l = LocalLogger(tag)
        l.addAppender(DEFAULT)
        return l
    }


    override fun levels(an: String): Iterable<String> {
        val app = appenders.get(an)
        if(app!=null) return app.levels

        return ArrayList()
    }


    override fun levels(an: String, levels: Iterable<String>) {
        val app = appenders.get(an)
        if(app!=null) {
            val ol = app.levels
            app.levels.clear()
            levels.each { app.addLevel(it) }
            observeAppenders.fireUpdate(an, "levels", ol, levels)
        }
    }


    override fun cfg(an: String, tag: String, levels: Iterable<String>) {
        val cfg = AppenderConfig(tag)
        levels.each { cfg.addLevel(it) }
    }
    fun buffer(name:String) : Appender = BufferedAppender(name)

    override fun track(appender: String, obs:Observer<LogEntry>) {
        val app = appenders.get(appender)
        if(app!=null) {
            app.addObserver(obs)
        }
    }
    override fun untrack(appender: String, obs:Observer<LogEntry>) {
        val app = appenders.get(appender)
        if(app!=null) {
            app.removeObserver(obs)
        }
    }
}

class LogFactory(val mgr:LoggerManager) {
    fun logger(tag:String) : Logger = mgr.logger(tag)

    class object {
        private val LOGGING = LogFactory(LocalLoggerManager())
        fun logger(tag:String) : Logger = LOGGING.logger(tag)
    }
}

