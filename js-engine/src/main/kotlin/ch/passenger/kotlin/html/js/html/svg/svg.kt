package ch.passenger.kotlin.html.js.html.svg

import ch.passenger.kotlin.html.js.html.Tag
import ch.passenger.kotlin.html.js.html.ROOT_PARENT
import js.debug.console
import js.dom.html.window
import js.dom.core.Node
import ch.passenger.kotlin.html.js.html.DOMEvent
import java.util.StringBuilder
import ch.passenger.kotlin.html.js.html.Text
import java.util.HashMap
import ch.passenger.kotlin.html.js.html.each
import java.util.ArrayList
import ch.passenger.kotlin.html.js.html.EventTarget
import ch.passenger.kotlin.html.js.html.EventManager
import ch.passenger.kotlin.html.js.html.EventTypes

/**
 * Created by Duric on 18.08.13.
 */


val nsSvg = "http://www.w3.org/2000/svg"

class SVG(override val extend:Extension,id:String?) : SvgElement("svg", id), Extended,ShapeContainer,ViewBox {
    override val me: SvgElement = this
    override val position: Position = Position(px(0),px(0));
    override var svgPT: SVGPoint? = null

    override val transforms: MutableList<Transform> = ArrayList();
    {
        attributes.att("xmlns",nsSvg)
        attributes.att("version","1.1")
    }


    protected override fun writeSvgContent() {
        writeExtend()
        writeTransform()
    }


    public fun createSVGPoint() : SVGPoint {
        if(node==null) throw IllegalStateException()
        val svg = node as SVGSVGElement
        return svg.createSVGPoint()
    }


    override fun isSVG(): Boolean = true
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

    fun circle(cx:Number, cy:Number, r:Number, id:String?, init:Circle.()->Unit): Circle {
        val c = Circle(cx.px(), cy.px(), r.px(), id)
        c.init()
        me.addChild(c)
        return c
    }

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

    fun group(id:String?=null, init:Group.()->Unit) : Group{
        val  g = Group(id)
        g.init()
        me.addElement(g)
        return g
    }

    fun svgtext(x:Length, y:Length,id:String?=null,init:SvgText.()->Unit) : SvgText {
        val t = SvgText(Position(x,y), id)
        t.init()
        me.addElement(t)
        return t
    }

    fun line(x1:Length, y1:Length, x2:Length, y2:Length,id:String?=null,init:Line.()->Unit): Line {
        val l = Line(x1,y1,x2,y2,id)
        l.init()
        me.addElement(l)
        return l
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
fun Number.percent() : Length = Length(this.toDouble(), Measure.percent)

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

    fun findParentSvg() :SVG? {
        if(isSVG()) return this as SVG
        val p = parent
        if(p is SvgElement) return p.findParentSvg()?:null
        return null
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

    open fun isSVG() : Boolean = false

    protected abstract fun writeSvgContent()


    protected override final fun preRefreshHook() {
        writeSvgContent()
    }
}

public enum class Measure {
em ex px inch cm mm pt pc percent
}


trait SvgLocatable {
    val me : SvgElement
    var svgPT : SVGPoint?

    fun client2locale(x:Number,y:Number) : SVGPoint {
        //console.log("convert $x,$y")
        if(me.node==null) throw IllegalStateException()
        val loc = me.node!! as SVGLocatable
        val ctm = loc.getScreenCTM()
        //console.log("screen ctm for ", me.id(), " is", ctm)
        //console.log("inverse screen ctm for ", me.id(), " is", ctm.inverse())
        var sp = svgPT
        if(sp ==null) {
            var svg : SVGSVGElement? = me.findParentSvg()?.node as SVGSVGElement
            if(svg==null) throw IllegalStateException()
            sp = svg?.createSVGPoint()
        }
        svgPT = sp
        if(sp !=null) {
            if(sp!=null && sp!!.x!=null)
                sp!!.x = x.toDouble()
            if(sp!=null && sp!!.y!=null)
                sp!!.y = y.toDouble()
            val res = sp?.matrixTransform(ctm.inverse())!!
            return res
        }
        throw IllegalStateException()
    }

    private fun findSvg()
}
class Length(val value:Double, val measure:Measure=Measure.px) {
    public fun toString() : String {
      var name = measure.name()
      if(measure==Measure.percent) name = "%"
      return "$value$name"
    }
}

class Position(var x:Length,var y:Length)
class Extension(var w:Length,var h:Length)
class Rounding(var rx:Length,var ry:Length)
trait Paint {val value : String }
class TransparentPaint(override val value:String="transparent") : Paint

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



trait Shape : Transformed {
    override val me : SvgElement

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
    var opacity : Double?
    var stroke_opacity : Double?

    fun writeStroke() {
        console.log("write stroke: ${stroke?.value}")
        if(stroke!=null)
        me.attributes.att("stroke", stroke?.value?:"")
        if(stroke_width!=null)
            me.attributes.att("stroke-width", "${stroke_width?.value}${stroke_width?.measure?.name()}"?:"")
        if(opacity!=null)
            me.attribute("opacity", "$opacity")
        if(stroke_opacity!=null)
            me.attribute("stroke-opacity", "$stroke_opacity")
    }
    fun stroke(p:Paint) = stroke = p
    fun black() : Paint = ANamedColor("black")
    fun transparent() : Paint = TransparentPaint()
    fun red() : Paint = ANamedColor("red")
    fun orange() : Paint = ANamedColor("orange")

}

trait Filled : Shape {
    var  fill : Paint?
    var fill_opacity : Double?
    fun writeFill() {
        console.log("write fill: ${fill?.value}")
        if(fill!=null)
            me.attributes.att("fill", fill?.value?:"")
        if(fill_opacity!=null)
            me.attribute("fill-opacity", "$fill_opacity")
    }
    fun fill(p:Paint) = fill = p
    fun noFill() = fill = TransparentPaint()
}


abstract class StrokeAndFill(name:String,id:String?) : SvgElement(name, id),Stroked,Filled {
    override var stroke: Paint? = null
    override var stroke_width: Length? = null
    override var fill: Paint? = null
    override var svgPT: SVGPoint? = null
    override val transforms: MutableList<Transform> = ArrayList()
    override val me: SvgElement = this
    override var opacity: Double? = null
    override var stroke_opacity: Double? = null
    override var fill_opacity: Double? = null

    protected override final fun writeSvgContent() {
        writeTransform()
        writeFill()
        writeStroke()
        writeStrokeFill()
    }

    abstract fun writeStrokeFill()
}

class Rect(override val position : Position, override val extend : Extension, id:String?) :
StrokeAndFill("rect", id), Extended,Rounded {
    override val rounding : Rounding = Rounding(px(0), px(0))


    override fun writeStrokeFill() {
        writePosition()
        writeExtend()
        writeRounded()
    }
}

trait Transformed : SvgLocatable {
    override val me: SvgElement
    val transforms : MutableList<Transform>


    fun writeTransform() {
        if(transforms.size()==0) me.attributes.remove("transform")
        else {
            val sb = StringBuilder()
            transforms.each {
                sb.append("${it.kind}(${it.value})")
                sb.append(" ")
            }
            me.attributes.att("transform", sb.toString())
        }
    }

    fun clearTransforms() {
        transforms.clear()
    }


    fun matrix(a:Number,b:Number,c:Number,d:Number,e:Number,f:Number) {
        transforms.add(TrMatrix(a,b,c,d,e,f))
    }

    fun translate(x:Number,y:Number) {
        transforms.add(TrTranslate(x,y))
    }

    fun rotate(a:Number,x:Number,y:Number) {
        transforms.add(TrRotate(a,x,y))
    }

    fun scale(x:Number,y:Number) {
        transforms.add(TrScale(x,y))
    }

    fun<T:Transform> animate(from:T, to:T, init:AnimateTransform<T>.()->Unit): AnimateTransform<T> {
        val a = AnimateTransform(from, to, 1.sec())
        a.init()
        me.addElement(a)
        return a
    }

}

class Duration(val length:Number, val unit:String) {
    public fun toString() : String = "$length$unit"
}

fun Number.sec() : Duration  = Duration(this, "s")
fun Number.ms() : Duration  = Duration(this, "ms")

class AnimateTransform<T:Transform>(var from:T, var to:T, var dur:Duration, var start:Duration=0.sec(), id:String?=null)
: SvgElement("animateTransform", id), EventManager {
    var repeatCount : Number = -1


    protected override fun writeSvgContent() {
        attribute("attributeName", "transform")
        attribute("attributeType", "XML")
        attribute("type", from.kind)
        attribute("from", from.value)
        attribute("to", to.value)
        if(dur.length.toInt() > 0)
        attribute("dur", "$dur")
        if(start.length.toInt() > 0)
            attribute("start", "$start")
        if(repeatCount.toInt() < 0)
            attribute("repeatCount", "indefinite")
        else attribute("repeatCount", "$repeatCount")
    }

    fun begin(cb:(DOMEvent)->Unit) = add(EventTypes.begin, cb)
    fun end(cb:(DOMEvent)->Unit) = add(EventTypes.end, cb)
}

abstract class Transform(val value:String, val kind:String)

class TrMatrix(val a:Number,val b:Number,val c:Number,val d:Number,val e:Number,val f:Number)
: Transform("$a $b $c $d $e $f", "matrix")
class TrScale(val x:Number, val y:Number) : Transform("$x $y", "scale")
class TrTranslate(val x:Number, val y:Number) : Transform("$x $y", "translate")
class TrRotate(val angle:Number, val x:Number, val y:Number) : Transform("$angle $x $y", "rotate")
class TrSkewX(val x:Number) : Transform("$x", "skewX")
class TrSkewY(val y:Number) : Transform("$y", "skewY")


class Group(id:String?) : SvgElement("g", id), ShapeContainer, Transformed {
    override val me: SvgElement = this
    override val transforms: MutableList<Transform> = ArrayList()
    override var svgPT: SVGPoint? = null


    protected override fun writeSvgContent() {
        writeTransform()
    }
}

class Circle(val cx:Length, val cy:Length, val r:Length, id:String?) : StrokeAndFill("circle", id) {
    override fun writeStrokeFill() {
        writeLength("cx", cx)
        writeLength("cy", cy)
        writeLength("r", r)
    }
}

class Line(var x1:Length, var y1:Length, var x2:Length, var y2:Length,id:String?=null) : SvgElement("line",id), Stroked {
    protected override fun writeSvgContent() {
        writeLength("x1", x1)
        writeLength("y1", y1)
        writeLength("x2", x2)
        writeLength("y2", y2)

        writeStroke()
        writeTransform()
    }
    override var stroke: Paint? = null
    override var stroke_width: Length? = null
    override var opacity: Double? = null
    override var stroke_opacity: Double? = null
    override val me: SvgElement = this


    override val transforms: MutableList<Transform> = ArrayList()
    override var svgPT: SVGPoint? = null
}

class Path(id:String?) : StrokeAndFill("path", id) {
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


    override fun writeStrokeFill() {

    }
}

class SvgText(override val position: Position, id:String?=null) : StrokeAndFill("text", id), Positioned,Transformed {

    fun text(s:String) {
        addChild(Text(s))
    }


    override fun writeStrokeFill() {
        writePosition()
    }
}

class SvgSpan(override val position: Position, id:String?=null) : StrokeAndFill("tspan", id), Positioned {
    fun text(s:String) {
        addChild(Text(s))
    }

    override fun writeStrokeFill() {
        writePosition()
    }
}

class ANamedColor( override val name: String ) : NamedColor {
    override val value: String = name
}