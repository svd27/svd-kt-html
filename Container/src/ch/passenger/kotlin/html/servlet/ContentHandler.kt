package ch.passenger.kotlin.html.servlet

import java.util.HashMap
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import java.util.StringTokenizer
import java.util.HashSet
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 11.07.13
 * Time: 15:00
 * To change this template use File | Settings | File Templates.
 */
public abstract class ContentHandler(val element : String) {
    val handlers : MutableSet<ContentHandler> = HashSet()

    fun handle(req: HttpServletRequest?, resp: HttpServletResponse?, path : List<String>) : Unit {
        if(iWant(path)) {
            service(req, resp, path)
            return
        }
        else if(path.size()>0) {
            for(h in handlers) {
                if(h.iWant(path)) {
                    h.handle(req, resp, path)
                    return
                }
            }
        }
        val om = ObjectMapper()
        val error = om.createObjectNode()
        error?.put("error", "no handler found for path: ${path}")
        resp?.setContentType("application/json")
        resp?.setContentLength(-1)
        resp?.setStatus(501)
        val pw = resp?.getWriter()
        om.writeValue(pw, error)
        pw?.flush()
        pw?.close()

    }

    open fun iWant(pe : List<String>) : Boolean {
        return false
    }

    open fun service(req: HttpServletRequest?, resp: HttpServletResponse?, path : List<String>) {
        throw UnsupportedOperationException()
    }
    fun add(ch:ContentHandler) {
        handlers.add(ch)
    }
}