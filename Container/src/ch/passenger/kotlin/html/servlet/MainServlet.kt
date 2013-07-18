package ch.passenger.kotlin.html.servlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.fasterxml.jackson.core.JsonFactory
import java.io.StringWriter
import java.util.HashMap
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.ArrayList
import ch.passenger.kotlin.symbolon.Word
import ch.passenger.kotlin.symbolon.Universe
import ch.passenger.kotlin.symbolon.Populator
import com.fasterxml.jackson.databind.JsonNode
import ch.passenger.kotlin.symbolon.TypeWord
import kotlin.properties.Delegates
import java.util.StringTokenizer


/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 24.06.13
 * Time: 20:07
 */
class MainServlet : HttpServlet() {
    public override fun init() {
        super<HttpServlet>.init()
        Populator.populate()
        if(getServletContext()?.getAttribute("Universe")==null) {
            getServletContext()?.setAttribute("Universe", Universe)
        }
    }

    protected override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val tok = StringTokenizer(req?.getPathInfo(), "/")
        val path : MutableList<String> = ArrayList(10)
        for(pe in tok) {
            path.add(pe.toString())
        }
        RootHandler.handle(req!!, resp!!, path)
    }

    private fun handle(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val p = req?.getPathInfo();
        val tok = StringTokenizer(p, "/")
        val path = ArrayList<String>(5)
        for(t in tok) {
            path.add(t.toString())
        }


    }


    protected fun test( req: HttpServletRequest?, resp: HttpServletResponse?){
        println("PI: " + req?.getPathInfo())
        println("QS: " + req?.getQueryString())
        resp?.setContentType("application/json")
        resp?.setContentLength(-1)

        val pw = resp?.getWriter()


        val v : MutableMap<String,String> = HashMap()

        v["name"] = "sasa"
        v["nick"] = "svd"

        val l : MutableList<Description> = ArrayList<Description>()
        l.add(Description("sasa", "svd"))
        l.add(Description("han", "han"))

        val om : ObjectMapper = ObjectMapper()
        val sw : StringWriter = StringWriter(1024)


        val s = om.writeValueAsString(l)
        println(s)
        om.writeValue(sw, l)
        println(sw.toString())


        pw?.write(sw.toString())
        pw?.flush()
        pw?.close()
    }


}

object RootHandler : ContentHandler("/") {

    {
        val ch = object : ContentHandler("words") {
            override fun iWant(pe: List<String>): Boolean {
                return pe.size()==1 && pe.first().equals(element)
            }


            override fun service(req: HttpServletRequest, resp: HttpServletResponse, path: List<String>) {
                //resp?.setContentLength(-1)
                resp.setContentType("application/json")


                val pw = resp.getWriter()

                val om : ObjectMapper = ObjectMapper()
                val sw : StringWriter = StringWriter(1024)

                val an = om.createArrayNode()!!
                for(w in Universe.iterate()) {
                    an.add(serWord(w))
                }


                val s = om.writeValueAsString(an)
                println(s)
                pw?.write(s as String)
                pw?.flush()
                pw?.close()
            }
        }
        add(ch)
        add(LoginHandler())

    }
}

fun serWord(w : Word) : JsonNode {
    val om : ObjectMapper = ObjectMapper()
    val nw = om.createObjectNode()
    if(nw==null) throw IllegalStateException()
    nw.put("id", w.id.urn)
    nw.put("name", w.name)
    nw.put("clazz", w.javaClass.getName())
    nw.put("loadState", "GOOD")
    if(!(w is TypeWord))
        nw.put("kind", w.kind.id.urn)

    if(w.qualities.size()>0) {
        val arrayNode = om.createArrayNode()
        if(arrayNode==null) throw IllegalStateException()
        for( qw in w.qualities) {
            arrayNode.add(qw.id.urn)
        }
    }

    return nw
}

class Description(val name : String, val nick : String) {

}