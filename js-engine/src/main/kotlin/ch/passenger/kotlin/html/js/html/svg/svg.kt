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

/**
 * Created by Duric on 18.08.13.
 */


val nsSvg = "http://www.w3.org/2000/svg"

class SVG(override val extend:Extension,id:String?) : SvgElement("svg", id), Extended,ShapeContainer,ViewBox,Styled {
    override val me: SvgElement = this
    override val position: Position = Position(px(0),px(0));
    override val transform: StringBuilder = StringBuilder()
    override var svgPT: SVGPoint? = null
    override val styles: MutableMap<String, String> = HashMap();

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
        console.log("convert $x,$y")
        if(me.node==null) throw IllegalStateException()
        val loc = me.node!! as SVGLocatable
        val ctm = loc.getScreenCTM()
        console.log("screen ctm for ", me.id(), " is", ctm)
        console.log("inverse screen ctm for ", me.id(), " is", ctm.inverse())
        var sp = svgPT
        if(sp ==null) {
            var svg : SVGSVGElement? = me.findParentSvg()?.node as SVGSVGElement
            if(svg==null) throw IllegalStateException()
            sp = svg?.createSVGPoint()
        }
        svgPT = sp
        if(sp !=null) {
            console.log("sp before transform ${sp?.x} ${sp?.y}")
            if(sp!=null && sp!!.x!=null)
                sp!!.x = x.toDouble()
            if(sp!=null && sp!!.y!=null)
                sp!!.y = y.toDouble()
            val res = sp?.matrixTransform(ctm.inverse())!!
            console.log("sp after transform ${sp?.x} ${sp?.y}")
            return res
        }
        throw IllegalStateException()
    }

    private fun findSvg()
}
class Length(val value:Double, val measure:Measure=Measure.px)

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



trait Styled {
    val me : SvgElement
    val styles : MutableMap<String,String>

    fun addStyle(property:String, value:String) {
        styles.put(property,value)
    }

    fun addStyle(property:String, value:Length) {
        styles.put(property,"${value.value}${value.measure.name()}")
    }

    fun writeStyle() {
        val sb = StringBuilder()
        for(k in styles.keySet()){
            sb.append("$k: ${styles.get(k)}; ")
        }
        me.attribute("style", sb.toString())
    }
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

    fun writeStroke() {
        console.log("write stroke: ${stroke?.value}")
        if(stroke!=null)
        me.attributes.att("stroke", stroke?.value?:"")
        if(stroke_width!=null)
            me.attributes.att("stroke-width", "${stroke_width?.value}${stroke_width?.measure?.name()}"?:"")
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
    fun noFill() = fill = TransparentPaint()
}


abstract class StrokeAndFill(name:String,id:String?) : SvgElement(name, id),Stroked,Filled,Styled {
    override var stroke: Paint? = null
    override var stroke_width: Length? = null
    override var fill: Paint? = null
    override var svgPT: SVGPoint? = null
    override val transform: StringBuilder = StringBuilder()
    override val styles: MutableMap<String, String> = HashMap()

    override val me: SvgElement = this


    protected override final fun writeSvgContent() {
        writeTransform()
        writeFill()
        writeStroke()
        writeStrokeFill()
        writeStyle()
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
    val transform : StringBuilder

    fun writeTransform() {
        me.attribute("transform", transform.toString())
    }

    fun matrix(a:Number,b:Number,c:Number,d:Number,e:Number,f:Number) {
        transform.append("matrix($a $b $c $d $e $f)")
    }
}

class Group(id:String?) : SvgElement("g", id), ShapeContainer, Transformed,Styled {
    override val me: SvgElement = this
    override val styles: MutableMap<String, String> = HashMap()
    override val transform: StringBuilder = StringBuilder()

    override var svgPT: SVGPoint? = null


    protected override fun writeSvgContent() {
        writeTransform()
    }
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