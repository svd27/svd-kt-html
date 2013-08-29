package ch.passenger.kotlin.html.js.html

/**
 * Created by sdju on 22.08.13.
 */
class BorderLayout(id:String?=null, init:BorderLayout.()->Unit) : FlowContainer("div", id) {
    var center : Div? = null
    var north : Div? = null
    var west : Div? = null
    var east : Div? = null
    var south : Div? = null
    var middle : Div? = null;

    {
        addStyle("display", "flex", "-webkit-flex")
        addStyle("flex-flow", "column")
        addStyle("-webkit-flex-flow", "column")
        this.init()
    }

    fun createMiddle() : Div {
        if(middle!=null) return middle!!
        middle = div {
            addStyle("display", "-webkit-flex",  "flex")
            addStyle("flex-flow", "row")
            addStyle("-webkit-flex-flow", "row")
            addStyle("-webkit-order", "2")
            addStyle("order", "2")
            addStyle("-webkit-flex-grow", "3")
            addStyle("flex-grow", "3")
        }

        return  middle!!
    }

    fun center(init:Div.()->Unit): Div {
        if(center!=null) center?.detach()
        if(middle==null) createMiddle()

        val c = middle!!.div() {
            addStyle("order","2")
            addStyle("flex-grow", "4")
            addStyle("-webkit-order", "2")
            addStyle("-webkit-flex-grow", "4")
            init()
        }
        center = c
        middle?.addChild(c!!)
        return c
    }

    fun north(init:Div.()->Unit): Div {
        if(north!=null) north?.detach()

        val c = div {
            addStyle("order", "1")
            addStyle("flex-grow", "1")
            addStyle("-webkit-order", "1")
            addStyle("-webkit-flex-grow", "1")
            init()
        }
        north = c
        addChild(c)
        return c
    }

    fun west(init:Div.()->Unit) {
        if(west!=null) west?.detach()

        if(middle==null) createMiddle()

        val c = middle?.div() {
            addStyle("order", "1")
            addStyle("flex-grow", "1")
            addStyle("-webkit-order", "1")
            addStyle("-webkit-flex-grow", "1")
            init()
        }
        west = c
        middle?.addChild(c!!)
    }

    fun east(init:Div.()->Unit) {
        if(east!=null) east?.detach()

        if(middle==null) createMiddle()

        val c = middle?.div() {
            addStyle("order", "3")
            addStyle("flex-grow", "1")
            addStyle("-webkit-order", "3")
            addStyle("-webkit-flex-grow", "1")
            init()
        }
        east = c
        middle?.addChild(c!!)
    }

    fun south(init:Div.()->Unit) {
        if(south!=null) south?.detach()

        val c = div {
            addStyle("order", "3")
            addStyle("flex-grow", "1")
            addStyle("-webkit-order", "3")
            addStyle("-webkit-flex-grow", "1")
            init()
        }
        south = c
        addChild(c)
    }
}