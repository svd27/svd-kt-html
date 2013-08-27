package ch.passenger.kotlin.html.js.test

import ch.passenger.kotlin.html.js.html.FlowContainer
import ch.passenger.kotlin.html.js.model.AbstractObserver
import ch.passenger.kotlin.html.js.html.Span
import ch.passenger.kotlin.html.js.model.Model
import ch.passenger.kotlin.html.js.model.Observer
import java.util.HashSet

/**
 * Created by sdju on 26.08.13.
 */
class CoordInfoComponent(private val parent: FlowContainer, private val model: CoordInfoModel) : AbstractObserver<CoordInfo>() {
    var screenX: Span? = null
    var screenY: Span? = null
    var clientX: Span? = null
    var clientY: Span? = null
    var svgX: Span? = null
    var svgY: Span? = null

    {
        model.addObserver(this)
        val m = model
        val that = this
        parent.div {
            div {
                div {
                    text("screen")
                    that.screenX = span {
                        if(m.value?.screenX != null)
                            text("x(${m.value?.screenX?.toFixed(2)})")
                    }
                    that.screenY = span {
                        if(m.value?.screenY != null)
                            text("y(${m.value?.screenY?.toFixed(2)})")
                    }
                }
                div{
                    text("client")
                    that.clientX = span {
                        if(m.value?.clientX != null)
                            text("x(${m.value?.clientX?.toFixed(2)})")
                    }
                    that.clientY = span {
                        if(m.value?.clientY != null)
                            text("y(${m.value?.clientY?.toFixed(2)})")
                    }
                }
                div {
                    text("svg")
                    that.svgX = span {
                        if(m.value?.svgX != null)
                            text("x(${m.value?.svgX?.toFixed(2)})")
                    }

                    that.svgY = span {
                        if(m.value?.svgY != null)
                            text("y(${m.value?.svgY?.toFixed(2)})")
                    }
                }
            }
        }
    }


    override fun added(t: CoordInfo) {
        all()
    }
    override fun removed(t: CoordInfo) {
        all()
    }
    override fun updated(t: CoordInfo, prop: String, old: Any?, nv: Any?) {
        all()
    }

    fun all() {
        rCX(); rCY(); rSX(); rSY(); rVX(); rVY()
    }

    fun rCX() {
        clientX?.clear()
        clientX?.text("x(${model.value?.clientX}(")
        clientX?.dirty = true
    }

    fun rCY() {
        clientY?.clear()
        clientY?.text("y(${model.value?.clientY})")
        clientY?.dirty = true
    }

    fun rSX() {
        screenX?.clear()
        screenX?.text("x(${model.value?.screenX})")
        screenX?.dirty = true
    }

    fun rSY() {
        screenY?.clear()
        screenY?.text("y(${model.value?.screenY})")
        screenY?.dirty = true
    }

    fun rVX() {
        svgX?.clear()
        svgX?.text("x(${model.value?.svgX})")
        svgX?.dirty = true
    }

    fun rVY() {
        svgY?.clear()
        svgY?.text("y(${model.value?.svgY}(")
        svgY?.dirty = true
    }
}

class CoordInfoModel() : Model<CoordInfo> {
    protected override val observers: MutableSet<Observer<CoordInfo>> = HashSet()
    protected override var _value: CoordInfo? = null


    protected override fun checkValue(nv: CoordInfo?, ov: CoordInfo?): CoordInfo? {
        if(ov == null) {
            fireAdd(nv!!)
        }
        if(nv == null) {
            fireRemove(ov!!)
            fireDelete(ov!!)
        }

        val oc = ov!!
        val nc = nv!!

        if(oc.screenX != nc.screenX) {
            fireUpdate(nc, "screenX", oc.screenX, nc.screenX)
        }

        if(oc.screenY != nc.screenY) {
            fireUpdate(nc, "screenY", oc.screenY, nc.screenY)
        }

        if(oc.clientX != nc.clientX) {
            fireUpdate(nc, "clientX", oc.clientX, nc.clientX)
        }

        if(oc.clientY != nc.clientY) {
            fireUpdate(nc, "clientY", oc.clientY, nc.clientY)
        }

        if(oc.svgX != nc.svgX) {
            fireUpdate(nc, "svgX", oc.svgX, nc.svgX)
        }

        if(oc.svgY != nc.svgY) {
            fireUpdate(nc, "svgY", oc.svgY, nc.svgY)
        }

        return nv
    }
}

class CoordInfo(var screenX: Number, var screenY: Number, var clientX: Number, var clientY: Number, var svgX: Number, var svgY: Number)