package ch.passenger.kotlin.jetty

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.AbstractNetworkConnector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.servlet.ServletContextHandler
import javax.servlet.Servlet
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.http.HttpSession
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.Holder
import ch.passenger.kotlin.basis.BosorkSession
import ch.passenger.kotlin.basis.BosorkApp
import ch.passenger.kotlin.basis.URN
import ch.passenger.kotlin.basis.BosorkRequest
import net.engio.mbassy.bus.MBassador
import ch.passenger.kotlin.basis.BosorkResponse
import ch.passenger.kotlin.basis.PublishEnvelope
import net.engio.mbassy.bus.BusConfiguration
import java.util.HashMap
import com.google.inject.servlet.ServletModule
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.DefaultServlet
import javax.servlet.ServletContextListener
import javax.servlet.ServletContextEvent
import java.util.EnumSet
import javax.servlet.DispatcherType
import java.io.File
import javax.servlet.ServletContext
import org.slf4j.LoggerFactory
import java.util.ArrayList
import ch.passenger.kotlin.basis.BosorkError
import java.nio.file.Files
import java.io.FileInputStream
import java.io.BufferedInputStream
import com.fasterxml.jackson.databind.ObjectMapper
import ch.passenger.kotlin.basis.LoginRequest
import com.fasterxml.jackson.databind.node.ObjectNode
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener
import java.util.EventListener
import javax.servlet.http.HttpSessionListener
import javax.servlet.http.HttpSessionEvent
import com.fasterxml.jackson.databind.JsonNode
import javax.servlet.http.HttpSessionBindingListener
import javax.servlet.http.HttpSessionActivationListener
import javax.servlet.http.HttpSessionBindingEvent
import javax.servlet.ServletConfig
import net.engio.mbassy.listener.Handler as handler
import ch.passenger.kotlin.basis.SessionFactory

/**
 * Created by sdju on 25.07.13.
 */

val log = LoggerFactory.getLogger(javaClass<BosorkServletSession>().getPackage()!!.getName())!!

fun jetty(cfg : Server.()->Unit) : Server {
    val server = Server()

    server.cfg()

    return server
}

fun Server.connectors(cfg : Server.() -> Array<Connector>) {
    setConnectors(cfg())
}

fun Server.handlers(cfg : Server.() -> Handler) {
    setHandler(cfg())
}


fun ServletContextHandler.servlets(cfg : ServletContextHandler.() -> Map<String,Class<Servlet?>>) {
    cfg().entrySet().forEach { addServlet(it.value, it.key) }
}

fun ServletContextHandler.socket(cfg : ServletContextHandler.() -> Pair<String,Class<BosorkWebsocketAdapter>>) {
    val pair = cfg()
    val wsc = WebSocketCreator {
        (req,resp) ->
        val ctor = pair.second.getConstructor(javaClass<HttpSession>())
        ctor.newInstance(req?.getSession())
    }
    class WSSServlet() : BosorkWebsocketServlet(wsc)
    val sh = ServletHolder(object : BosorkWebsocketServlet(wsc){})
    //addServlet(javaClass<WSSServlet>(), pair.first)
    addServlet(sh, pair.first)
}




fun AbstractNetworkConnector.configure(cfg : AbstractNetworkConnector.()->Unit) {
    cfg()
}

class BosorkServletSession(override val app : BosorkApp, override var token: URN) :
BosorkSession, HttpSessionBindingListener, HttpSessionActivationListener {
    override val attributes: MutableMap<String, Any?> = HashMap()
    override val reqBus: MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus: MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
    override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap();
    val websocket : BosorkWebsocketAdapter? = null

    class object {
        val SESSION_ATTRIBUTE :String = "BOSORK-SESSION"
    }


    public override fun valueBound(event: HttpSessionBindingEvent?) {
        log.info("im ${token.urn} officially part of this session ${event?.getSource()}: ${event?.getName()}")
    }
    public override fun valueUnbound(event: HttpSessionBindingEvent?) {
        log.info("im ${token.urn} dead for this session ${event?.getSource()}: ${event?.getName()}")
    }
    public override fun sessionWillPassivate(se: HttpSessionEvent?) {
        log.info("${token.urn} my session ${se?.getSource()}: ${se?.getSession()} will go to sleep")
    }
    public override fun sessionDidActivate(se: HttpSessionEvent?) {
        log.info("${token.urn} my session ${se?.getSource()}: ${se?.getSession()} is waking up")
    }
}

class DefaultWebAppSessionProvider : SessionFactory {
    override fun createSession(token: URN, app: BosorkApp): BosorkSession {
        return BosorkServletSession(app, token)
    }
}


trait BosorkWebResource {
    fun createHeadTag() : String
}

class JSResource(val path:String, val prefix:String) : BosorkWebResource {

    override fun createHeadTag(): String {
        return """
        <script type="text/javascript" src="${prefix}/${path}"></script>
        """
    }
}

class CSSResource(val path:String, val prefix:String) : BosorkWebResource {

    override fun createHeadTag(): String {
        return """
        <link rel="stylesheet" type="text/css" href="${prefix}/${path}">
        """
    }
}

abstract class BosorkServlet : HttpServlet() {
    protected abstract fun serve(req: HttpServletRequest, resp: HttpServletResponse)
    protected abstract val methods : Set<String>
    protected val om: ObjectMapper  = ObjectMapper()

    protected override final fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        try {
            val httpSession = req!!.getSession(true)
            log.info("received req for ${httpSession?.getId()}")
            if(methods.contains(req!!.getMethod())) serve(req, resp!!)
            else throw IllegalStateException(req.getMethod() + " not supported")
        } catch(e: Exception) {
            log.error(e.getMessage(), e)
            writeResponse(error(e), req!!, resp!!)
        }

    }

    protected fun writeResponse(node:ObjectNode, req:HttpServletRequest, resp:HttpServletResponse) {
        om.writerWithDefaultPrettyPrinter()!!.writeValue(resp.getWriter()!!, node)
        resp.getWriter()!!.flush()
        resp.getWriter()!!.close()
    }

    protected fun error(t:Throwable): ObjectNode {
        val error = om.createObjectNode()!!
        error.put("state", "error")
        error.put("cause", t.javaClass.getName())
        error.put("message", t.getMessage())
        val resp = om.createObjectNode()!!
        resp.put("error", error)
        return resp
    }

    protected fun ok(payLoad:JsonNode?) : ObjectNode{
        val ok = om.createObjectNode()!!
        ok.put("state", "ok")
        if(payLoad!=null)
        ok.put("payload", payLoad)
        return ok
    }
}

class InitServlet : BosorkServlet() {
    protected override val methods: Set<String> = setOf("GET")

    protected override fun serve(req: HttpServletRequest, resp: HttpServletResponse) {
        val pw = resp.getWriter()!!
        val app = req.getServletContext()?.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule

        resp.setContentType("text/html")
        resp.setCharacterEncoding("UTF-8")

        val resources = StringBuilder()
        app.resources.forEach {
            resources.append(it.createHeadTag())
        }

        val doc =
                """
<!DOCTYPE html>
<html>
<head>
    <title>${app.app.id.urn}</title>
    <meta charset="UTF-8">
    ${resources.toString()}
</head>
<body>
</body>
</html>
                """

        pw.write(doc)
    }
}

class ResourceServlet : BosorkServlet() {
    protected override fun serve(req: HttpServletRequest, resp: HttpServletResponse) {
        resource(req, resp)
    }

    protected override val methods: Set<String> = setOf("GET")

    fun resource(req: HttpServletRequest, resp: HttpServletResponse) {
        val app = req.getServletContext()?.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule
        val pt = req.getPathInfo()?.substring(1)
        val f = File(app.root, pt!!)
        if(!f.exists() || !f.isFile() || !f.canRead()) throw BosorkError("cant find or read ${pt}")

        var mime : String ? = Files.probeContentType(f.toPath())

        if(mime==null) mime = "text/plain"

        val fis = BufferedInputStream(FileInputStream(f))
        resp.setContentType(mime)
        val out = resp.getOutputStream()!!

        fis.copyTo(out, 1024)
        fis.close()
        out.close()

    }
}

class LoginServlet() : BosorkServlet() {
    protected override val methods: Set<String> = setOf("POST")


    public override fun init(config: ServletConfig?) {
        super<BosorkServlet>.init(config)
        val app = config!!.getServletContext()!!.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule
        val l = object : Any() {
            [handler]
            fun listen(r: BosorkResponse) {
                r.session.token = r.
            }
        }
        app.app.listen(l)
    }
    protected override fun serve(req: HttpServletRequest, resp: HttpServletResponse) {
        requestLogin(req, resp)
    }
    private fun requestLogin(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.setContentType("text/json")
        resp.setCharacterEncoding("UTF-8")
        val hs = req.getSession()
        if(hs!=null) {
            if(hs.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE)!=null) {
                val bs : BosorkServletSession = hs.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE) as BosorkServletSession
                if(bs.token!=null) {
                    val res = om.createObjectNode()
                    res?.put("token", bs.token.urn)
                    om.writerWithDefaultPrettyPrinter()!!.writeValue(resp.getWriter(), res)
                    return
                }
            }
        }

        val app = req.getServletContext()?.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule

        val node = om.readTree(req.getReader())
        val user = node?.path("user")?.textValue()!!
        val password = node?.path("pwd")?.textValue()!!
        val cid = node?.path("cid")?.longValue()?:-1.toLong()
        val lreq = LoginRequest(app.app, app.app.auth!!.login, cid, user, password)
        app.app.request(lreq)
        writeResponse(ok(null), req, resp)
    }
}




class AppServletModule(val app:BosorkApp, val resources:Array<BosorkWebResource>, val root:File) {
    {
        app.listen(this)
    }



    fun init(ctx:ServletContext) {
        ctx.setAttribute(APP_ATTRIBUTE, this)
    }

    fun servlets(ctx:ServletContextHandler) {
        log.info("adding ${javaClass<InitServlet>()} on path: ${app.id.specifier}")
        ctx.addServlet(javaClass<InitServlet>(), "/"+app.id.specifier)
        log.info("adding ${javaClass<ResourceServlet>()} on path: ${"/"+app.id.specifier+"/resource"}")
        ctx.addServlet(javaClass<ResourceServlet>(), "/"+app.id.specifier+"/resource/*")
        val l = object : HttpSessionListener {
            public override fun sessionCreated(se: HttpSessionEvent?) {
                log.info("session created: ${se?.getSource()}")
                val s : HttpSession
            }
            public override fun sessionDestroyed(se: HttpSessionEvent?) {
                //val hs : HttpSession = se!!.getSource()!! as HttpSession

            }
        }
        ctx.getSessionHandler()!!.addEventListener(l)
    }

    fun sockets(ctx:ServletContextHandler) {
        ctx.socket {
            class Adaptor(s : HttpSession) : BosorkWebsocketAdapter(s) {

                public override fun onWebSocketConnect(sess: Session?) {
                    session
                }


                public override fun onWebSocketClose(statusCode: Int, reason: String?) {

                }


                public override fun onWebSocketText(message: String?) {
                    //ignore
                }
            }
            Pair("/events", javaClass<Adaptor>() as Class<BosorkWebsocketAdapter>)
        }
    }

    class object {
        val APP_ATTRIBUTE : String = "BOSORK_APP"
    }
}

class AppFactory(private val appmodule : AppServletModule, val port:Int) {
    var server : Server? = null


    fun init() {
        server = jetty() {
            connectors {
                val c = ServerConnector(this)
                c.setPort(port)
                array(c)
            }

            handlers {
                val ctx : ServletContextHandler = ServletContextHandler(ServletContextHandler.SESSIONS)

                ctx.setContextPath("/")
                log.info("!!!CL: ${ctx.getClassLoader()}")

                //http://www.javaintegrations.com/2012/08/using-embedded-jetty-with-guice-servlet.html
                ctx.addEventListener(object : ServletContextListener {
                    public override fun contextInitialized(ctx : ServletContextEvent?) {
                        log.info("calling appmodule ${appmodule.app.id.urn} init")
                        appmodule.init(ctx?.getServletContext()!!)
                    }
                    public override fun contextDestroyed(ctx : ServletContextEvent?) {
                        println("${ctx?.getSource()} destroyed")
                    }
                })

                //val dispatches = EnumSet.allOf(javaClass<DispatcherType>())
                val dispatches = EnumSet.of(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST)
                println(dispatches)

                ctx.addServlet(javaClass<DefaultServlet>(), "/")
                log.info("calling appmodule ${appmodule.app.id.urn} servlets")
                appmodule.servlets(ctx)

                ctx
            }
        }

    }
}
