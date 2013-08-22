package ch.passenger.kotlin.html.js.html.svg

import ch.passenger.kotlin.html.js.html.Tag
import ch.passenger.kotlin.html.js.html.ROOT_PARENT
import js.debug.console
import js.dom.html.window
import js.dom.core.Node
import ch.passenger.kotlin.html.js.html.DOMEvent
import java.util.StringBuilder

/**
 * Created by Duric on 18.08.13.
 */


val nsSvg = "http://www.w3.org/2000/svg"

class SVG(override val extend:Extension,id:String?) : SvgElement("svg", id), Extended,ShapeContainer,ViewBox {
    override val me: SvgElement = this
    override val position: Position = Position(px(0),px(0));


    {
        attributes.att("xmlns",nsSvg)
        attributes.att("version","1.1")
    }


    protected override fun preRefreshHook() {
        writeExtend()
    }
}

trait ViewBox {
    val me : SvgElement

    fun viewBox(x:Double,y:Double,w:Double,h:Double){
        me.attribute("viewBox","$x $y $w $h")
    }

    fun viewBox(x:Int,y:Int,w:Int,h:Int){
        me.attribute("viewBox","$x $y $w $h")
    }

    fun pARnone() {
        me.attribute("preserveAspectRatio","none")
    }

    fun pARxMinYMin () {
        me.attribute("preserveAspectRatio","xMinYMin")
    }
}

trait ShapeContainer {
    val me : SvgElement

    fun rect(x:Number,y:Number,w:Number,h:Number,id:String?=null,init:Rect.()->Unit) : Rect {
        return rect(x.px(),y.px(),w.px(),h.px(),id,init)
    }

    fun rect(x:Length,y:Length,w:Length,h:Length,id:String?=null,init:Rect.()->Unit) : Rect {
        val r = Rect(Position(x,y), Extension(w,h), id)
        r.init()
        me.addElement(r)
        return r
    }

    fun path(id:String?=null,init:Path.()->Unit) : Path {
        val p = Path(id)
        p.init()
        me.addElement(p)
        return p
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

fun Number.px() : Length = Length(this.toDouble(), Measure.px)

abstract class SvgElement(name:String,id:String?) : Tag(name, id) {
    public override fun createNode(): Node? {
        if(hidden) return null
        console.log("create svg $name in ${parent?.id()}: ${parent?.node?.nodeName}")
        if(parent!=null && (parent?.node!=null||parent==ROOT_PARENT)) {
            val doc = window.document
            node = doc.createElementNS(nsSvg,name)!!

            attributes.refresh(node)
            initListeners()
            if(parent!=ROOT_PARENT) insertIntoParent()
        }

        return node
    }

    public fun addElement(e:SvgElement) {
        addChild(e)
    }

    public fun attribute(name:String,value:String) {
        attributes.att(name,value)
    }

    public fun attribute(name:String) : String? {
        return attributes.att(name)?.value
    }

    override fun mouseenter(cb: (DOMEvent) -> Unit) {
        mouseover(cb)
    }


    override fun mouseleave(cb: (DOMEvent) -> Unit) {
        mouseout(cb)
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
    var stroke_width : Length?

    fun writeStroke() {
        console.log("write stroke: ${stroke?.value}")
        if(stroke!=null)
        me.attributes.att("stroke", stroke?.value?:"")
        if(stroke_width!=null)
            me.attributes.att("stroke-width", "${stroke_width?.value}"?:"")
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
    override var stroke_width: Length? = null
    override var fill: Paint? = null
}

class Rect(override val position : Position, override val extend : Extension, id:String?) :
StrokeAndFill("rect", id), Extended,Rounded {
    override val me: SvgElement = this
    override val rounding : Rounding = Rounding(px(0), px(0))

    protected override fun preRefreshHook() {
        writePosition()
        writeExtend()
        writeFill()
        writeStroke()
        writeRounded()
    }
}

class Path(id:String?) : StrokeAndFill("path", id) {
    override val me: SvgElement = this
    private var buffer: StringBuilder = StringBuilder()


    fun l(x:Int,y:Int) : Path {
        cC("l", x, y)
        return this
    }

    fun L(x:Int,y:Int) : Path {
        cC("L", x, y)
        return this
    }

    fun m(x:Int,y:Int) : Path {
        cC("m", x, y)
        return this
    }

    fun M(x:Int,y:Int) : Path {
        cC("M", x, y)
        return this
    }

    private fun cC(cmd:String, x:Int, y:Int) {
        buffer.append("$cmd $x $y ")
    }

    public fun done() {
        attributes.att("d", buffer.toString())
    }

    protected override fun preRefreshHook() {
        writeFill()
        writeStroke()
    }
}

class ANamedColor( override val name: String ) : NamedColor {
    override val value: String = name
}