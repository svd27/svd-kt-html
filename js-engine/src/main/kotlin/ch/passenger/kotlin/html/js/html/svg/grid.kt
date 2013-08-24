package ch.passenger.kotlin.html.js.html.svg

import ch.passenger.kotlin.html.js.html.DOMMouseEvent
import java.util.HashMap
import ch.passenger.kotlin.html.js.html.HtmlElement

/**
 * Created by sdju on 20.08.13.
 */
class Grid(val parent:ShapeContainer, val w:Int, val h:Int, val rows:Int, val columns:Int, val id:String?=null) {
    var innerLines : Paint = ANamedColor("black")
    var outerLines : Paint = ANamedColor("black")
    var outerWidth : Length? = null
    var innerWidth : Length?  =null
    var group : Group? = null
    val cw = w/columns
    val ch = h/rows
    var legend : Boolean = false
    val fh : Length = (cw*.7).px()
    val fhs :Length = (fh.value/3).px()


    fun rotate(a:Number) {
        group?.rotate(a, w/2, h/2)
        group?.dirty = true
    }

    fun normTransform() {
        group?.clearTransforms()
        group?.dirty = true
    }

    fun paint() {
        val that = this@Grid
        if(group!=null) {
            group?.detach()
        }

        group = parent.group(id) {
            rect(0, 0, that.w, that.h) {
                stroke = that.outerLines
                stroke_width = that.outerWidth
                noFill()
            }
            for(i in 1..(that.rows-1)) {
                val x1 = (i * that.cw).px()
                val y1 = 0.px()
                line(x1, y1, x1, that.h.px()) {
                    stroke = that.innerLines
                    if(i%3==0) stroke_width = that.outerWidth
                }
            }
            for(i in 1..(that.columns-1)) {
                val x1 = 0.px()
                val y1 = (i * that.cw).px()
                line(x1, y1, that.w.px(), y1) {
                    stroke = that.innerLines
                    if(i%3==0) stroke_width = that.outerWidth
                }
            }
            if(that.legend) {
                val fh = (that.cw*.7).px()
                for(i in 1..(that.rows)) {
                    val dx = (i-1) * that.cw
                    for(j in 1..that.columns) {
                        val dy = (j-1) * that.ch
                        svgtext((dx+.5*that.cw).px(), (dy+.5*that.ch).px()) {
                            addStyle("font-size", fh)
                            addStyle("text-anchor", "middle")
                            addStyle("dominant-baseline", "middle")
                            attribute("opacity", "0.7")
                            stroke = ANamedColor("grey")
                            text("$i,$j")
                        }
                    }
                }
            }
        }
        group?.dirty=true
    }

    val cells : Array<Array<Cell?>?> = Array<Array<Cell?>?>(rows) {null}

    fun cell(row:Int,col:Int) : Cell {
        if(row>rows || col>columns) throw IllegalStateException()

        if(cells[row-1]==null) cells[row-1] = Array<Cell?>(9) {null}

        val cm = cells[row-1]!!
        if(cm[col-1] ==null) cm[col-1] = Cell(row, col, this)

        return cm[col-1]!!
    }

    fun cellTextCenter(c:Cell, init:SvgText.()->Unit) {
        //if(c.center!=null) c.center?.detach()
        val dx = (c.col-1).toDouble() * cw
        val dy = (c.row-1).toDouble() * ch
        c.clear()
        val that = this
        val svgText = group?.svgtext((dx + .5 * that.cw).px(), (dy + .5 * that.ch).px()) {
            addStyle("font-size", that.fh)
            addStyle("text-anchor", "middle")
            addStyle("dominant-baseline", "middle")
            init()
        }
        c.coreValue = svgText
        svgText?.dirty = true
    }


    fun cell(e : DOMMouseEvent) : Cell? {
        val cp = group!!.client2locale(e.clientX, e.clientY)
        if(cp.x<0 || cp.y<0) return null
        val col = cp.x/cw
        val row = cp.y/ch

        if(row<0 || row>=rows) return null
        if(col<0 || col>=columns) return null

        return cell(row.toInt()+1, col.toInt()+1)
    }
}

enum class Subcells(val idx : Int) {
    nw : Subcells(0)
    n : Subcells(1)
    ne : Subcells(2)
    e : Subcells(3)
    se  : Subcells(4)
    s : Subcells(5)
    sw : Subcells(6)
    w : Subcells(7)
    c  : Subcells(8)
}

fun<T> Array<T>.each(cb:(T)->Unit) {
    for(e in this) cb(e)
}

fun<T> Array<T>.eachIdx(cb:(Int,T)->Unit) {
    for(i in 0..(size-1)) cb(i, this[i])
}

class Cell(val row:Int,val col:Int, val grid:Grid) {
    val subcells : Array<SvgElement?> = Array<SvgElement?>(9) {null}
    var coreValue : SvgElement? = null
    var value : Number? = null
    val cw : Int = grid.cw
    val ch : Int = grid.ch
    val nfh : Length = grid.fh
    val sfh : Length = (nfh.value*.35).px()
    val cell : Group = grid.group!!.group("c$row$col") {}
    val origin : Position = Position(((col-1)*cw).px(), ((row-1)*ch).px());
    {
        val that = this
        cell.rect(origin.x.value, origin.y.value, that.cw, that.ch) {
            stroke(TransparentPaint()); noFill()
        }
    }



    fun remove(v:Number) {
        if(value!=null && value==v) {
            coreValue?.detach()
            value = null

        } else {
            val sub = subcells[v.toInt()]
            if(sub !=null) {
                sub.detach()
                subcells[v.toInt()] = null
            }
        }
        cell.dirty = true
    }

    fun clearSubs(refresh:Boolean=false) {
        subcells.each { if(it!=null) it.detach() }
        subcells.eachIdx { (i,v) -> subcells[i] = null }
        if(refresh) cell.dirty = true
    }

    fun clear() {
        clearSubs()
        if(coreValue!=null) coreValue?.detach()
        coreValue = null
        value = null
        cell.dirty = true
    }


    fun value(v:Number) {
        value = v
        val fh = nfh
        clearSubs()
        if(coreValue!=null) coreValue?.detach()

        val p = posCenter(0.0,0.0)

        val svgText = cell.svgtext(p.x, p.y) {
            addStyle("font-size", (fh.value).px())
            addStyle("text-anchor", "middle")
            addStyle("dominant-baseline", "middle")
            text("$v")
        }
        coreValue = svgText
        coreValue?.dirty = true
    }

    fun candidate(v:Number) {
        if(subcells[v.toInt()]!=null) return
        coreValue?.detach()

        var dx = 0.toDouble()
        var dy = 0.toDouble()
        val cw = grid.cw.toDouble()
        val ch = grid.ch.toDouble()
        val fh = sfh
        when(v) {
            1 -> {dx = -cw/4; dy=-ch/4;}
            2 -> {dx = 0.0; dy = -ch/4}
            3 -> {dx = cw/4; dy = -ch/4}
            4 -> {dx = cw/4; dy = 0.0}
            5 -> {dx = cw/4; dy = ch/4}
            6 -> {dx = 0.0; dy = ch/4}
            7 -> {dx = -cw/4; dy = ch/4}
            8 -> {dx = -cw/4; dy = 0.0}
            9 -> {dx = 0.0; dy = 0.0}
            else -> {dx = 0.0; dy = 0.0}
        }

        val p = posCenter(dx,dy)

        val svgText = cell.svgtext(p.x, p.y) {
            addStyle("font-size", fh)
            addStyle("text-anchor", "middle")
            addStyle("dominant-baseline", "middle")
            text("$v")
        }
        subcells[v.toInt()] = svgText
        cell.dirty = true
    }

    fun posCenter(xoff:Double, yoff:Double) : Position {
        return Position((xoff+(col)*grid.cw-grid.cw/2).px(), (yoff+(row)*grid.ch-grid.ch/2).px())
    }

    public fun toString() : String = "Cell($row,$col)"
}