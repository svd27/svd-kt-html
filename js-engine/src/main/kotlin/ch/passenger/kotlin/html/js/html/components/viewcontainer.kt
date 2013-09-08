package ch.passenger.kotlin.html.js.html.components

import ch.passenger.kotlin.html.js.html.Tag
import ch.passenger.kotlin.html.js.html.HtmlElement
import ch.passenger.kotlin.html.js.html.Div
import ch.passenger.kotlin.html.js.html.svg.px
import ch.passenger.kotlin.html.js.html.svg.percent
import ch.passenger.kotlin.html.js.html.BorderLayout
import java.util.HashMap
import ch.passenger.kotlin.html.js.logger.Logger
import ch.passenger.kotlin.html.js.logger.LogFactory

/**
 * Created by sdju on 27.08.13.
 */

open class ViewContainer(id:String?=null, protected var selected : String? = null) : Tag("div", id) {
    val log = LogFactory.logger("ViewContainer")
    override public fun addChild(v:HtmlElement)  {
        if (v is Tag) {
            doAddChild(v)
            v.addStyle("position", "absolute")
            v.addStyle("width", "100%")
            v.addStyle("height", "100%")
            if(children.size()>1) {
                log.debug("hiding ${v.id()} for now")
                v.addStyle("visibility", "hidden")
            }
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
        log.debug("view($id) selected: $selected")
        if(id==selected) return

        val sel = selected
        if (sel !=null) {
            val now = find(sel)
            if(now!=null && now is Tag) {
                log.debug("hiding ${now.id()}")
                now.addStyle("visibility", "hidden")
                now.addStyle("position", "absolute")
                now.removeClass("boz-view-selected")
                now.dirty = true
            }
        }

        val view = find(id)
        log.debug("target view $id returned ",view)
        if(view!=null) {
            if(view is Tag) {
                log.debug("showing ${view.name}:${view.id()}")
                selected = id
                view.addClass("boz-view-selected")
                view.addStyle("visibility", "visible")
                view.styles.remove("position")
                view.dirty = true
            }
        }
    }

}


enum class Gesture {
    enter leave click key
}

class TabbedView(g:Gesture, id:String?=null) : Tag("Div", id) {
    val log = LogFactory.logger("TabbedView")
    val layout = BorderLayout(){}
    val north:Div = layout.north {  }
    val vc : ViewContainer = ViewContainer()
    val center:Div = layout.center {  }
    val tabs : MutableMap<String,String> = HashMap()
    var gesture : Gesture = g
      set(v) {
          $gesture = v
          north.each {
              if (it is Tag) {
                  val t : Tag = it
                  when(v) {
                      Gesture.click -> it.click { tabbed(t) }
                      Gesture.enter -> it.mouseover { tabbed(t) }
                      else -> null
                  }
                  t.dirty = true
              }
          }
      }

    {
        north.addClass("bos-tabs")
        center.addChild(vc)
        addChild(layout)
    }

    fun tabbed(h:Tag) {
        log.debug("tabbed: ${h.id()}")
        val id = h.id()
        val cont = tabs.get(id)
        if (cont!=null) {
            log.debug("showing $cont")
            vc.view(cont)
        }
    }

    fun tab(s:String,c:Tag) {
        val h = Div()
        h.text(s)
        tab(h, c)
    }

    fun tab(header:Tag, content:Tag) {
        north.addChild(header)
        vc.addChild(content)
        when(gesture) {
            Gesture.click -> header.click { tabbed(header) }
            Gesture.enter -> header.mouseover { tabbed(header) }
            else -> null
        }
        tabs.put(header.id(), content.id())
        dirty = true
    }

    fun cfg(c:TabbedView.()->Unit) {
        c()
    }

}