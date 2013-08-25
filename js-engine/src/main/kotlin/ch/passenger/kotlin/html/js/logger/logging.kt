package ch.passenger.kotlin.html.js.logger

import js.debug.console
import java.util.ArrayList
import java.util.StringBuilder
import ch.passenger.kotlin.html.js.html.each
import java.util.HashMap
import java.util.HashSet
import ch.passenger.kotlin.html.js.model.Observable
import ch.passenger.kotlin.html.js.model.Observer

/**
 * Created by Duric on 25.08.13.
 */

class LogEntry(val tag:String, val level:String, val content:String)

abstract class Appender(val noop:Boolean=false) : Observable<LogEntry> {
    var allLevels : Boolean = true
    val levels : MutableSet<String> = HashSet()
    fun addLevel(level:String) {
        levels.add(level)
        allLevels = false
    }
    fun removeLevel(l:String) = levels.remove(l)

    abstract fun write(s:String)

    fun write(level:String, tag:String, content:String) {
        //TODO: add formatters
        write("$level$tag: $content\n")
        if(observers.size()>0) {
            fireAdd(LogEntry(tag, level, content))
        }
    }

    abstract fun currentContent() : String

    protected override val observers: MutableSet<Observer<LogEntry>> = HashSet()
}
class ConsoleAppender() : Appender() {
    override fun write(s: String) {
        console.log(s)
    }

    override fun currentContent(): String = ""
}

class NullAppender() : Appender(true) {

    override fun write(s: String) {

    }
    override fun currentContent(): String = ""
}

class BufferedAppender(val maxSize:Int=999999) : Appender() {
    val lines : MutableList<String> = ArrayList()
    var size :Int = 0


    override fun write(s: String) {
        if(s.length()>maxSize) throw IllegalArgumentException()
        if(size+s.length()>maxSize) {
            lines.remove(0)
            write(s)
        } else {
            size += s.length()
            lines.add(s)
        }
    }
    override fun currentContent(): String {
        val sb = StringBuilder()
        lines.each {
            sb.append(it)?.append("\n")
        }
        return sb.toString()
    }
}

class Logger private (val tag:String, private var appenders:MutableList<Appender>) {
    public fun log(level:String, vararg content:Any?) {
        var payload : String? = null
        appenders.each {
            if(it.noop && it.levels.contains(level)) {
                if (payload==null) {
                    val sb = StringBuilder()
                    content.each {
                        sb.append(it)
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
        private val appenders : MutableMap<String,Appender> = HashMap()
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

        fun appenders() : Set<String> = appenders.keySet()
        fun appender(name:String) : Appender? = appenders.get(name)
        fun loggers() : Collection<Logger> = TAGS.values()
    }
}