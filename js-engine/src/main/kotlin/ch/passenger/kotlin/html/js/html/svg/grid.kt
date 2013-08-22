package ch.passenger.kotlin.html.js.html.svg

/**
 * Created by sdju on 20.08.13.
 */
class Grid(val parent:ShapeContainer, val w:Int, val h:Int, val rows:Int, val columns:Int, val id:String?=null) {
    var innerLines : Paint = ANamedColor("black")
    var outerLines : Paint = ANamedColor("black")
    var outerWidth : Length? = null
    var innerWidth : Length?  =null
    fun paint(init:Path.()->Unit) {
        val that = this
        val rect = parent.rect(0, 0, w, h) {
            stroke = that.outerLines
            stroke_width = that.outerWidth
        }
        val path = parent.path(id) {
            stroke(that.innerLines)
            stroke_width = that.innerWidth
            val dx = that.w/that.rows
            val dy = that.h/that.columns

            M(0,0)
            for(i in 1..that.columns-1) {
                l(0,that.h)
                m(dx,-that.h)
            }

            M(0,0)
            for(i in 1..that.rows-1) {
                l(that.w,0)
                m(-that.w,dy)
            }
            done()
        }


        path.init()
    }
}