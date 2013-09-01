package ch.passenger.kotlin.html.js.binding

import js.dom.html.HTMLElement

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 01.09.13
 * Time: 12:46
 * To change this template use File | Settings | File Templates.
 */

enum class EventTypes {
    message mouseenter mouseleave click change mouseover mouseout mousemove keypress keydown keyup end begin
}


public native trait DOMEvent {
    public val target : HTMLElement
    public var data : Any?

    fun targetId() {
        target.id
    }

    fun preventDefault() = js.noImpl
}
