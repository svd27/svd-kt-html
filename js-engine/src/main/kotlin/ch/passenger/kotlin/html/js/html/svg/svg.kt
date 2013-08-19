package ch.passenger.kotlin.html.js.html.svg

import ch.passenger.kotlin.html.js.html.HtmlElement
import ch.passenger.kotlin.html.js.html.Tag
import js.debug.console
import js.dom.html.window
import ch.passenger.kotlin.html.js.html.MyWindow
import ch.passenger.kotlin.html.js.html.Callback
import js.dom.core.Node

/**
 * Created by Duric on 18.08.13.
 */
class SVG(override val extend:Extension,id:String?) : SvgElement("svg", id), Extended {
    override val me: SvgElement = this
    override val position: Position = Position(px(0),px(0));

    override fun writeContent(): String {

        return writeChildren()
    }

    fun rect(x:Length,y:Length,w:Length,h:Length,id:String?=null,init:Rect.()->Unit) : Rect {
        val r = Rect(Position(x,y), Extension(w,h), id)
        r.init()
        addChild(r)
        return r
    }


    override fun writeSvgContent() {
        writeExtend()
    }
}

fun px(v:Int) : Length {
    return Length(v.toDouble())
}

inline fun px() : Length = px(0)
inline fun cm(v:Double) : Length= Length(v, Measure.cm)
inline fun cm(v:Int) : Length= cm(v.toDouble())
inline fun percent(v:Double) : Length = Length(v,Measure.percent)
inline fun percent(v:Int) : Length = percent(v.toDouble())

abstract class SvgElement(name:String,id:String?) : Tag(name, id) {
    abstract fun writeSvgContent()
    override fun writeContent(): String {
        return writeChildren()
    }


    override fun preRender() {
        writeSvgContent()
    }

    fun mouseenter(cb:Callback) {
        val SESSION = (window as MyWindow)!!.bosork!!
        val aid = SESSION.actionHolder.add(cb)
        addClass("mouseenter")
        atts {
            att("data-enter-action", "${aid}")
        }
    }
    fun mouseleave(cb:Callback) {
        val SESSION = (window as MyWindow)!!.bosork!!
        val aid = SESSION.actionHolder.add(cb)
        addClass("mouseleave")
        atts {
            att("data-leave-action", "${aid}")
        }
    }
    fun click(cb:Callback) {
        val SESSION = (window as MyWindow)!!.bosork!!
        val aid = SESSION.actionHolder.add(cb)
        addClass("action")
        atts {
            att("data-action", "${aid}")
        }
    }
}

public enum class Measure {
em ex px inch cm mm pt pc percent
}



class Length(val value:Double, val measure:Measure=Measure.px)

class Position(var x:Length,var y:Length)
class Extension(var w:Length,var h:Length)
class Rounding(var rx:Length,var ry:Length)
trait Paint {val value : String }
trait Color : Paint
trait NamedColor : Color {
    val name: String
    override val value: String
        get() = name
}

trait RGBColor : Color {
    val r: Int
    val g: Int
    val b: Int
    override val value: String
        get() = "rgb($r,$g,$b)"
}





trait Shape {
    val me : SvgElement

    fun writeLength(name:String, v:Length) {
        me.attributes.att(name, "${v.value}${if(v.measure==Measure.percent) "%" else v.measure.name()}")
    }

}

trait Positioned : Shape {
    val position : Position

    fun writePosition() {
        writeLength("x", position.x)
        writeLength("y", position.y)
    }


    fun x(v:Double, m:Measure=Measure.px) : Positioned {
        position.x = Length(v,m)
        return this
    }

    fun y(v:Double, m:Measure=Measure.px) : Positioned {
        position.y = Length(v,m)
        return this
    }
}

trait Extended : Positioned {
    val extend : Extension


    fun writeExtend() {
        writeLength("width", extend.w)
        writeLength("height", extend.h)
    }
    fun w(v:Double, m:Measure=Measure.px) : Extended {
        extend.w = Length(v,m)
        return this
    }

    fun h(v:Double, m:Measure=Measure.px) : Extended {
        extend.h = Length(v,m)
        return this
    }
}

trait Rounded : Shape {
    val rounding : Rounding

    fun rx(v:Length) : Rounded {
        rounding.rx = v
        return this
    }

    fun ry(v:Length) : Rounded {
        rounding.ry = v
        return this
    }

    fun writeRounded() {
        if(rounding.rx.value != 0.toDouble())
            writeLength("rx", rounding.rx)
        if(rounding.ry.value != 0.toDouble())
            writeLength("rý", rounding.ry)
    }
}

trait Stroked : Shape {
    var stroke : Paint?

    fun writeStroke() {
        console.log("write stroke: ${stroke?.value}")
        if(stroke!=null)
        me.attributes.att("stroke", stroke?.value?:"")
    }
    fun stroke(p:Paint) = stroke = p

}

trait Filled : Shape {
    var  fill : Paint?
    fun writeFill() {
        console.log("write fill: ${fill?.value}")
        if(fill!=null)
            me.attributes.att("fill", fill?.value?:"")
    }
    fun fill(p:Paint) = fill = p
}


abstract class StrokeAndFill(name:String,id:String?) : SvgElement(name, id),Stroked,Filled {
    override var stroke: Paint? = null
    override var fill: Paint? = null


    protected override fun preRefreshHook(n: Node) {
        writeSvgContent()
    }
    override fun writeSvgContent() {
        writeFill()
        writeStroke()
    }
}

class Rect(override val position : Position, override val extend : Extension, id:String?) :
StrokeAndFill("rect", id), Extended,Rounded {
    override val me: SvgElement = this
    override val rounding : Rounding = Rounding(px(0), px(0))
    override fun writeSvgContent() {
        //TODO: cant call super implementation
        writeFill()
        writeStroke()
        writePosition()
        writeExtend()
        writeRounded()
    }
}

class ANamedColor( override val name: String ) : NamedColor {
    override val value: String = name
}