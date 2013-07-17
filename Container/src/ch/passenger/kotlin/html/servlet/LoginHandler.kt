package ch.passenger.kotlin.html.servlet

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.fasterxml.jackson.databind.ObjectMapper
import javax.servlet.ServletException

/**
 * Created by sdju on 16.07.13.
 */

class LoginHandler: ContentHandler("login") {

    override fun iWant(pe: List<String>): Boolean {
        return pe.first() == "login";
    }

    override fun service(req: HttpServletRequest, resp: HttpServletResponse, path: List<String>) {
        if(req.getMethod() != "POST") throw IllegalStateException()
        val reader = req.getReader()!!
        val om = ObjectMapper()!!
        val node = om.readTree(reader)

        val session = req.getSession(true)!!
        if(session.getAttribute("BOSORK-TOKEN") == null) {
            val token = token()
            session.setAttribute("BOSORK-TOKEN", token)
        }

        val res = om.createObjectNode()!!
        res.put("token", session.getAttribute("BOSORK-TOKEN")!! as String)

        resp.setContentType("application/json")
        val printWriter = resp.getWriter()!!
        val rs = om.writeValueAsString(res)
        printWriter.write(rs.toString())
        printWriter.flush()
        printWriter.close()
    }

    private fun token(): String {
        return "BOSORK:${System.nanoTime()}"
    }
}