package ch.passenger.kotlin.html.js.html.svg

/**
 * Created by sdju on 20.08.13.
 */
class Grid(val parent:ShapeContainer, val w:Int, val h:Int, val rows:Int, val columns:Int, val id:String?=null) {
    private var path : Path = Path(id)

    fun paint(init:Path.()->Unit) {
        val that = this
        path = parent.path(id) {
            stroke(ANamedColor("black"))
            val dx = that.w/that.rows
            val dy = that.h/that.columns

            M(0,0)
            for(i in 0..that.columns) {
                l(0,that.h)
                m(dx,0)
            }

            M(0,0)
            for(i in 0..that.rows) {
                l(that.w,0)
                m(0,dy)
            }
            done()
        }


        path.init()
    }
}