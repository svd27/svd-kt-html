package ch.passenger.kotlin.html.js.logger

import ch.passenger.kotlin.html.js.html.Select
import ch.passenger.kotlin.html.js.model.SelectionObservableAdapter
import ch.passenger.kotlin.html.js.html.util.Converter
import ch.passenger.kotlin.html.js.html.FlowContainer
import ch.passenger.kotlin.html.js.html.CheckBox
import ch.passenger.kotlin.html.js.model.Model
import ch.passenger.kotlin.html.js.model.Observer
import java.util.HashSet
import ch.passenger.kotlin.html.js.model.AbstractObserver
import ch.passenger.kotlin.html.js.model.AbstractSelectionModel
import ch.passenger.kotlin.html.js.model.SelectionModel
import ch.passenger.kotlin.html.js.html.Link
import ch.passenger.kotlin.html.js.html.InputText
import java.util.ArrayList
import ch.passenger.kotlin.html.js.html.svg.eachIdx
import ch.passenger.kotlin.html.js.html.each
import ch.passenger.kotlin.html.js.html.components.TabbedView
import ch.passenger.kotlin.html.js.html.components.Gesture
import ch.passenger.kotlin.html.js.html.Div
import ch.passenger.kotlin.html.js.html.BorderLayout
import ch.passenger.kotlin.html.js.html.Tag
import ch.passenger.kotlin.html.js.html.svg.percent
import ch.passenger.kotlin.html.js.html.svg.vw
import ch.passenger.kotlin.html.js.html.svg.vh

val log = Logger.logger("LogManager")

/**
 * Created by Duric on 26.08.13.
 */

fun<T> listOf(vararg t:T) : List<T> {
    val l = ArrayList<T>(t.size)
    t.each { l.add(it) }
    return l
}

class LogManager(id: String? = null) : FlowContainer("div", id) {
    val log = Logger.logger("logmanager")
    val shoutLevels : Select<String> = Select(object:AbstractSelectionModel<String>(listOf("DEBUG", "INFO", "WARN", "ERROR", "FATAL"), false){})
    val loggers: Select<Logger> = Select(SelectionObservableAdapter(Logger.observeLoggers, Logger.loggers()),
            object:Converter<Logger> {

                override fun convert2string(t: Logger): String {
                    return t.tag
                }
                override fun convert2target(s: String): Logger {
                    return Logger.logger(s)!!
                }
            })
    val appenders: Select<String> = Select(SelectionObservableAdapter(Logger.observeAppenders, Logger.appenders()));
    val allLevelsModel: Model<Boolean> = object : Model<Boolean> {

        private var last: Boolean? = null
        protected override var _value: Boolean?
            get() {
                var b: Boolean? = null
                val sel = appenders.selected()
                if(sel != null) {
                    val app = Logger.appender(sel)
                    if(app != null)  b = app.levels.contains("ALL")
                }
                b
            }
            set(v) {
                val sel = appenders.selected()
                if(sel == null) return
                val app = Logger.appender(sel)
                if(app == null) return
                last = app.levels.contains("ALL")
                last?:false
                if(v!=null && v!!) app.levels.add("ALL")
                else app.levels.remove("ALL")
            }
        protected override val observers: MutableSet<Observer<Boolean>> = HashSet();

        {
            appenders.model.addObserver(object:AbstractObserver<String>() {
                override fun loaded(t: String) {
                    fireUpdate(value?:false, "this", last, value)
                    last = value
                }
                override fun unloaded(t: String) {
                    fireUpdate(value?:false, "this", last, null)
                }
            })
        }
    }
    val chkAllLevels: CheckBox = CheckBox(allLevelsModel)
    val levelsModel: SelectionModel<String> = object: AbstractSelectionModel<String>(currentLevels(), true) {
        private val that = this@LogManager;
        {
            appenders.model.addObserver(object:AbstractObserver<String>() {

                override fun loaded(t: String) {
                    items.clear()
                    items.addAll(that.currentLevels())
                }
                override fun unloaded(t: String) {
                    items.clear()
                }
            })
        }


        public override fun refresh() {
            items.clear()
            items.addAll(that.currentLevels())
        }
    }

    val addLevel: Link = Link("+Level")
    val inpLevel: InputText = InputText()
    val inpAppender: InputText = InputText()
    val addAppender: Link = Link("+Appender")
    val inpShout : InputText = InputText()
    val layout : BorderLayout = BorderLayout() {}
    val track: Link = Link("Track")
    val logpane : LogPane = LogPane()
            ;

    {
        addClass("bos-popup")
        addClass("bos-logmanager")
        val that = this
        addLevel.click {
            val an = inpLevel.value()
            if(an != null && an.trim().length() > 0) {
                val target = appenders.selected()
                if(target != null) {
                    Logger.appender(target)?.addLevel(an)
                    levelsModel.add(an)
                }
            }
        }
        addAppender.click {
            val an = inpAppender.value()
            if(an != null && an.trim().length() > 0) {
                Logger.buffer(an)
            }
        }
        val tabs = TabbedView(Gesture.click, "logManagerTabs")
        val cl = Div().div {
            that.log.debug("adding loggers")
            +that.loggers
            +that.shoutLevels
            +that.inpShout
            a("shout") {
                click {
                    val l = that.loggers.selected()
                    val lvl = that.shoutLevels.selected()
                    if(l!=null && lvl!=null) {
                        l.log(lvl, that.inpShout.value())
                    }
                }
            }
        }

        tabs.tab("Loggers", cl)
        val ca = Div().div {
            addChild(that.appenders)
            label(that.inpAppender.id()) {
                text("Add Appender:")
            }
            addChild(that.inpAppender)
            addChild(that.addAppender)
            that.track.click {
                val app = that.appenders.selected()
                if(app!=null) {
                    that.logpane.appender = Logger.appender(app)
                }
            }
            +that.track
            a("stop tracking") {
                click { that.logpane.appender=null }
            }
        }
        tabs.tab("Appenders", ca)
        layout.north {
            addChild(tabs)
        }
        layout.center {
            +that.logpane
        }
        addChild(layout)
    }

    fun currentLevels(): Set<String> {
        val an = appenders.selected()
        if(an != null) {
            val app = Logger.appender(an)
            if(app != null) return app.levels
        }
        return HashSet()
    }
}

class LogPane(id:String?=null) : Tag("div", id)  {
    val log = Logger.logger("logmanager.logpane")
    var pane : Div = Div()
    val obs : AbstractObserver<LogEntry> = object : AbstractObserver<LogEntry>() {
        override fun added(t: LogEntry) {
            //log.debug("received: ${t.tag}:${t.level}")
            val slevel = pane.span() {
                text(t.level)
            }
            slevel.addClass("log")
            slevel.addClass(t.level)
            pane.span() {
                text(t.tag)
            }
            pane.span() {
                val e = t
                text("${e.date.getHours()}:${e.date.getMinutes()}:${e.date.getSeconds()}:${e.date.getMilliseconds()}")
            }
            pane.span() {
                val e = t
                text("${e.content}")
            }
            pane.br()
            pane.dirty = true
        }
    }
    var appender : Appender? = null
    set(a) {
        if($appender!=null) {
            appender?.removeObserver(obs)
            pane.detach()
            pane = Div()
            pane.addStyle("overflow", "auto")
            //TODO: weird creates [70vh] in chrome
            pane.addStyle("height", 70.vh())
            pane.addStyle("width", 70.vw())
            addChild(pane)
            dirty = true
        }
        $appender = a
        if(a != null) {
            a.addObserver(obs)
        }

    }
}