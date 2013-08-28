package ch.passenger.kotlin.html.js.logger

import js.debug.console
import java.util.ArrayList
import java.util.StringBuilder
import ch.passenger.kotlin.html.js.html.each
import java.util.HashMap
import java.util.HashSet
import ch.passenger.kotlin.html.js.model.Observable
import ch.passenger.kotlin.html.js.model.Observer
import ch.passenger.kotlin.html.js.binding.Date

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

abstract class Appender(var format:LogFormatter=DefaultLogFormatter(), val noop:Boolean=false) : Observable<LogEntry> {
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
        console.log("i want: ${iWant(level)} cfg: ${cfg(tag).accept(level)}")
        return iWant(level) && cfg(tag).accept(level)
    }

    open fun currentContent() : List<LogEntry> = ArrayList()

    protected override val observers: MutableSet<Observer<LogEntry>> = HashSet()
}
class ConsoleAppender(format:LogFormatter=DefaultLogFormatter()) : Appender(format) {
    override fun write(entry: LogEntry) {
        console.log(format.format(entry))
    }
}

class NullAppender() : Appender(DefaultLogFormatter(), true) {
    override fun write(entry: LogEntry) {}

    override fun want(tag: String, level: String): Boolean = false
}

class BufferedAppender(val maxSize:Int=999999, format:LogFormatter=DefaultLogFormatter()) : Appender(format) {
    val lines : MutableList<LogEntry> = ArrayList()


    override fun write(e:LogEntry) {
        if(lines.size()>maxSize) throw IllegalArgumentException()
        if(lines.size()==maxSize) {
            lines.remove(0)
        }
        lines.add(e)

        write(e)
    }


    override fun currentContent(): List<LogEntry> = lines
}

class Logger private (val tag:String, private var appenders:MutableList<Appender>) {
    public fun log(level:String, vararg content:Any?) {
        var payload : String? = null
        appenders.each {
            if(!it.noop && it.want(tag, level)) {
                if (payload==null) {
                    val sb = StringBuilder()
                    content.each {
                        if(it!=null)
                        sb.append(it)
                        else sb.append("null")
                    }
                    payload = sb.toString()
                }
                it.write(level, tag, payload!!)
            }
        }

    }

    public fun debug(vararg content:Any?) : Unit = log("DEBUG", content)
    public fun info(vararg content:Any?) : Unit = log("INFO", content)
    public fun warn(vararg content:Any?) : Unit = log("WARN", content)
    public fun error(vararg content:Any?) : Unit = log("ERROR", content)
    public fun fatal(vararg content:Any?) : Unit = log("FATAL", content)

    class object {
        val DEFAULT : Appender=ConsoleAppender()
        private val TAGS : MutableMap<String,Logger> = HashMap()
        private val appenders : MutableMap<String,Appender> = HashMap<String,Appender>()
        public val observeLoggers : Observable<Logger> = object : Observable<Logger> {
            protected override val observers: MutableSet<Observer<Logger>> = HashSet()
        }
        public val observeAppenders : Observable<String> = object : Observable<String> {
            protected override val observers: MutableSet<Observer<String>> = HashSet()
        }

        fun listOfApp(app:Appender): ArrayList<Appender> {
            val l = ArrayList<Appender>(1)
            l.add(app)
            return l
        }

        fun logger(tag:String) : Logger {
            if(!TAGS.containsKey(tag)) {
                TAGS.put(tag,Logger(tag, listOfApp(DEFAULT)))
                observeLoggers.fireAdd(TAGS.get(tag)!!)
            }
            return TAGS.get(tag)!!
        }

        fun buffer(name:String)  {
            if(name.length()==0) return
            if(!appenders.containsKey(name)) {
                appenders.put(name, BufferedAppender())
                observeAppenders.fireAdd(name)
            }
        }

        fun logAppender(log:Logger, name:String) {
            if(appenders.containsKey(name))
            log.appenders.add(appenders.get(name)!!)
        }

        fun unlogAppender(log:Logger, name:String) {
            if(appenders.containsKey(name))
                log.appenders.remove(appenders.get(name)!!)
        }

        fun removeAppender(name:String) {
            val app : Appender? = appenders.get(name)
            if (app!=null) {
                TAGS.values().each {
                    it.appenders.remove(app)
                }
                appenders.remove(name)
                observeAppenders.fireRemove(name)
                observeAppenders.fireDelete(name)
            }
        }

        fun silence(logger:Logger) {
            logger.appenders.clear()
        }

        fun appenders() : Set<String> {
            if(!appenders.containsKey("DEFAULT")) {
                appenders.put("DEFAULT", DEFAULT)
            }
            return appenders.keySet()
        }
        fun appender(name:String) : Appender? = appenders.get(name)
        fun loggers() : Collection<Logger> = TAGS.values()
        fun appenders(tag:String) : Collection<Appender> {
            return TAGS.get(tag)?.appenders?:ArrayList<Appender>()
        }
    }
}