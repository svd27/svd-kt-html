package ch.passenger.kotlin.html.js.html.svg

import ch.passenger.kotlin.html.js.html.DOMMouseEvent
import java.util.HashMap

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

    fun paint() {
        val that = this@Grid
        group = parent.group(id) {
            rect(0, 0, that.w, that.h) {
                stroke = that.outerLines
                stroke_width = that.outerWidth
                noFill()
            }
            path() {
                stroke(that.innerLines)
                stroke_width = that.innerWidth
                val dx = that.w/that.rows
                val dy = that.h/that.columns

                M(dx,0)
                for(i in 1..that.columns-1) {
                    l(0,that.h)
                    m(dx,-that.h)
                }

                M(0,dy)
                for(i in 1..that.rows-1) {
                    l(that.w,0)
                    m(-that.w,dy)
                }
                done()
            }
            if(that.legend) {
                val fh = (that.cw*.7).px()
                for(i in 1..that.rows) {
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

    fun clear() {
        subcells.each { if(it!=null) it.detach() }
        if(coreValue!=null) coreValue?.detach()
    }

    fun ne(s:String) {
        val ce = subcells[Subcells.ne.idx]
        if(ce !=null) {
            ce.detach()
            subcells[Subcells.ne.idx] = null
        }
        val p = posCenter(-grid.cw.toDouble()/4, -grid.ch.toDouble()/4)
        val g = grid
        val svgText = g.parent.svgtext(p.x, p.y) {
            addStyle("font-size", (g.fh.value / 4).px())
            addStyle("text-anchor", "middle")
            addStyle("dominant-baseline", "middle")
        }
        subcells[Subcells.ne.idx] =svgText
        subcells[Subcells.ne.idx]?.dirty = true
    }

    fun value(s:String) {
        subcells.each {
            if(it!=null) it.detach()
        }
    }

    fun posCenter(xoff:Double, yoff:Double) : Position {
        return Position((xoff+(col-1)*grid.cw-grid.cw/2).px(), (yoff+(row-1)*grid.ch-grid.ch/2).px())
    }

    public fun toString() : String = "Cell($row,$col)"
}