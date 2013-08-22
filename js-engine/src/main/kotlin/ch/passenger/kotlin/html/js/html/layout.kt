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
        addStyle("display: flex; display: -webkit-flex; flex-flow: column; -webkit-flex-flow: column;")
        this.init()
    }

    fun createMiddle() : Div {
        if(middle!=null) return middle!!
        middle = div {
            addStyle("display: -webkit-flex; display: flex; flex-flow: row; -webkit-flex-flow: row; -webkit-order: 2; order: 2; flex-grow: 3; -webkit-flex-grow: 3;")
        }

        return  middle!!
    }

    fun center(init:Div.()->Unit) {
        if(center!=null) center?.detach()
        if(middle==null) createMiddle()

        val c = middle?.div() {
            addStyle("order: 2; flex-grow: 4;")
            addStyle("-webkit-order: 2; -webkit-flex-grow: 4;")
            init()
        }
        center = c
        middle?.addChild(c!!)
    }

    fun north(init:Div.()->Unit) {
        if(north!=null) north?.detach()

        val c = div {
            addStyle("order: 1; flex-grow: 1;")
            addStyle("-webkit-order: 1; -webkit-flex-grow: 1;")
            init()
        }
        north = c
        addChild(c)
    }

    fun west(init:Div.()->Unit) {
        if(west!=null) west?.detach()

        if(middle==null) createMiddle()

        val c = middle?.div() {
            addStyle("order: 1; flex-grow: 1;")
            addStyle("-webkit-order: 1; -webkit-flex-grow: 1;")
            init()
        }
        west = c
        middle?.addChild(c!!)
    }

    fun east(init:Div.()->Unit) {
        if(east!=null) east?.detach()

        if(middle==null) createMiddle()

        val c = middle?.div() {
            addStyle("order: 3; flex-grow: 1;")
            addStyle("-webkit-order: 3; -webkit-flex-grow: 1;")
            init()
        }
        east = c
        middle?.addChild(c!!)
    }

    fun south(init:Div.()->Unit) {
        if(south!=null) south?.detach()

        val c = div {
            addStyle("order: 3; flex-grow: 1;")
            addStyle("-webkit-order: 3; -webkit-flex-grow: 1;")
            init()
        }
        south = c
        addChild(c)
    }
}