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

    val cells : MutableMap<Int,MutableMap<Int,Cell>> = HashMap()

    fun cell(row:Int,col:Int) : Cell {
        if(row>rows-1 || col>columns-1) throw IllegalStateException()

        if(cells.get(row)==null) cells.put(row, HashMap())
        val cm = cells.get(row)!!
        if(cm.get(col)==null) cm.put(col, Cell(row, col, this))

        return cm.get(col)!!
    }

    fun cellTextCenter(c:Cell, init:SvgText.()->Unit) {
        if(c.center!=null) c.center?.detach()
        val dx = (c.col-1) * cw
        val dy = (c.row-1) * ch

        val that = this
        group?.svgtext((dx+.5*that.cw).px(), (dy+.5*that.ch).px()) {
            addStyle("font-size", that.fh)
            addStyle("text-anchor", "middle")
            addStyle("dominant-baseline", "middle")
            init()
        }
    }

    fun cellTextNE(row:Int, col:Int, s:String) {

    }

    fun cell(e : DOMMouseEvent) : Cell {
        val cp = group!!.client2locale(e.clientX, e.clientY)
        val col = cp.x/cw
        val row = cp.y/ch

        return Cell(row.toInt(), col.toInt(), this)
    }
}

class Cell(val row:Int,val col:Int, val grid:Grid) {
    var center : SvgElement? = null
    var ne : SvgElement? = null
}