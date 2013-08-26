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
    val shoutLevels : Select<String> = Select(object:AbstractSelectionModel<String>(listOf("debug", "info", "warn", "error", "fatal"), false){})
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
                    if(app != null)  b = app.allLevels
                }
                b
            }
            set(v) {
                val sel = appenders.selected()
                if(sel == null) return
                val app = Logger.appender(sel)
                if(app == null) return
                last = app.allLevels
                last?:false
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
        {
            appenders.model.addObserver(object:AbstractObserver<String>() {

                override fun loaded(t: String) {
                    items.clear()
                    items.addAll(currentLevels())
                }
                override fun unloaded(t: String) {
                    items.clear()
                }
            })
        }


        public override fun refresh() {
            items.clear()
            items.addAll(currentLevels())
        }
    }

    val addLevel: Link = Link("+Level")
    val inpLevel: InputText = InputText()
    val inpAppender: InputText = InputText()
    val addAppender: Link = Link("+Appender")
    val inpShout : InputText = InputText()

            ;
    fun currentLevels(): Set<String> {
        val an = appenders.selected()
        if(an != null) {
            val app = Logger.appender(an)
            if(app != null) return app.levels
        }
        return HashSet()
    }
    {
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
        border {
            north {
                div {
                    log.debug("adding loggers")
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
                div {
                    addChild(that.appenders)
                    label(that.inpAppender.id()) {
                        text("Add Appender:")
                    }
                    addChild(that.inpAppender)
                    addChild(that.addAppender)
                }
            }
        }
    }
}