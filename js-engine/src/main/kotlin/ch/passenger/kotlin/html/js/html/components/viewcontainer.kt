package ch.passenger.kotlin.html.js.html.components

import ch.passenger.kotlin.html.js.html.Tag
import ch.passenger.kotlin.html.js.html.HtmlElement
import ch.passenger.kotlin.html.js.html.Div

/**
 * Created by sdju on 27.08.13.
 */

open class ViewContainer(id:String?=null, protected var selected : String? = null) : Tag("div", id) {
    override public fun addChild(v:HtmlElement)  {
        if (v is Tag) {
            doAddChild(v)
            if(children.size()>1)
                v.addStyle("visibility", "hidden")
            else view(v.id())
        }
    }

    public fun div(id:String?=null, init:Div.()->Unit) {
        val div = Div(id)
        div.init()
        addChild(div)
    }

    fun view() : String? = selected
    fun view(id:String) {
        if(id==selected) return

        val sel = selected
        if (sel !=null) {
            val now = find(sel)
            if(now!=null && now is Tag) {
                now.addStyle("visibility", "hidden")
                now.removeClass("boz-view-selected")
                now.dirty = true
            }
        }

        val view = find(id)
        if(view!=null) {
            if(view is Tag) {
                selected = id
                view.addClass("boz-view-selected")
                view.addStyle("visibility", "visible")
                view.dirty = true
            }
        }
    }

}