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
import ch.passenger.kotlin.basis.SessionFactoryProvider
import ch.passenger.kotlin.json.Jsonifier
import ch.passenger.kotlin.basis.BosorkService
import ch.passenger.kotlin.basis.ServiceProvider
import com.fasterxml.jackson.databind.node.ArrayNode
import ch.passenger.kotlin.basis.AnonymousAuthService
import ch.passenger.kotlin.basis.AuthProvider
import java.io.StringReader

/**
 * Created by sdju on 25.07.13.
 */

val logJetty = LoggerFactory.getLogger(javaClass<BosorkServletSession>().getPackage()!!.getName())!!

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
        ctor.newInstance(req?.getSession()!!)
    }
    class WSSServlet() : BosorkWebsocketServlet(wsc)
    val sh = ServletHolder(object : BosorkWebsocketServlet(wsc){})
    //addServlet(javaClass<WSSServlet>(), pair.first)
    addServlet(sh, pair.first)
}

fun ServletContextHandler.socket(app:BosorkApp, cfg : ServletContextHandler.(BosorkApp) -> Pair<String,Class<BosorkWebsocketAdapter>>) {
    val pair = cfg(app)
    val wsc = WebSocketCreator {
        (req,resp) ->
        val ctor = pair.second.getConstructor(javaClass<HttpSession>())
        val qs = req!!.getQueryString()!!
        logJetty.info("WS create: ${qs}")
        val token = URN.token(qs)
        logJetty.info("WS query: ${token.urn}")
        val bosorkSession = app.session(token)!! as BosorkServletSession
        ctor.newInstance(bosorkSession.httpSession)
    }
    class WSSServlet() : BosorkWebsocketServlet(wsc)
    val sh = ServletHolder(object : BosorkWebsocketServlet(wsc){})
    //addServlet(javaClass<WSSServlet>(), pair.first)
    addServlet(sh, pair.first)
}

fun AbstractNetworkConnector.configure(cfg : AbstractNetworkConnector.()->Unit) {
    cfg()
}

class BosorkServletSession(override val app : BosorkApp, override var token: URN, val httpSession:HttpSession) :
BosorkSession, HttpSessionBindingListener, HttpSessionActivationListener {
    override val attributes: MutableMap<String, Any?> = HashMap()
    override val reqBus: MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus: MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
    override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap();
    var events : BosorkWebsocketAdapter? = null
    var responses : BosorkWebsocketAdapter? = null

    {
        httpSession.setAttribute(SESSION_ATTRIBUTE, this)
    }

    class object {
        val SESSION_ATTRIBUTE :String = "BOSORK-SESSION"
    }


    public override fun valueBound(event: HttpSessionBindingEvent?) {
        logJetty.info("im ${token.urn} officially part of this session ${event?.getSource()}: ${event?.getName()}")
    }
    public override fun valueUnbound(event: HttpSessionBindingEvent?) {
        logJetty.info("im ${token.urn} dead for this session ${event?.getSource()}: ${event?.getName()}")
    }
    public override fun sessionWillPassivate(se: HttpSessionEvent?) {
        logJetty.info("${token.urn} my session ${se?.getSource()}: ${se?.getSession()} will go to sleep")
    }
    public override fun sessionDidActivate(se: HttpSessionEvent?) {
        logJetty.info("${token.urn} my session ${se?.getSource()}: ${se?.getSession()} is waking up")
    }


    val eventer = object : Any() {
        [handler]
        fun listen(env:PublishEnvelope) {
            val payload = Jsonifier.serialise(env.event, app)
            events?.send(Jsonifier.asString(payload))
        }
    }

    val responder = object : Any() {
        [handler]
        fun listen(resp:BosorkResponse) {
            logJetty.info("sending $resp")
            responses?.send(Jsonifier.asString(Jsonifier.serialise(resp, app)))
        }
    }

    {
        respBus.subscribe(responder)
        respBus.addErrorHandler {
            log.error(it?.getMessage(), it?.getCause())
        }
    }

    override fun channelRequested(owner: URN, channel: MBassador<PublishEnvelope>) {
        channel.subscribe(eventer)
        channel.addErrorHandler {
            log.error(it?.getMessage(), it?.getCause())
        }
    }
}

class DefaultWebAppSessionFactoryProvider : SessionFactoryProvider {

    override fun provider(app: BosorkApp): SessionFactory {
        return DefaultWebAppSessionProvider(app)
    }
}

class DefaultWebAppSessionProvider(val app : BosorkApp) : SessionFactory {
    override fun createSession(req:LoginRequest, token: URN): BosorkSession {
        val wr = req as WebLoginRequest
        return BosorkServletSession(app, token, wr.req.getSession(true)!!)
    }
}



abstract class BosorkServlet : HttpServlet() {
    protected abstract fun serve(req: HttpServletRequest, resp: HttpServletResponse)
    protected abstract val methods : Set<String>
    protected val om: ObjectMapper  = ObjectMapper()

    protected override final fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        try {
            val httpSession = req!!.getSession(true)
            logJetty.info("received req for ${httpSession?.getId()}")
            if(methods.contains(req.getMethod())) serve(req, resp!!)
            else throw IllegalStateException(req.getMethod() + " not supported")
        } catch(e: Exception) {
            logJetty.error(e.getMessage(), e)
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

class WebWorkerServlet : BosorkServlet() {
    protected override fun serve(req: HttpServletRequest, resp: HttpServletResponse) {
        resource(req, resp)
    }

    protected override val methods: Set<String> = setOf("GET")

    fun resource(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.setContentType("text/javascript")
        val asm = req.getServletContext()?.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule
        asm.workerResources.forEach {
            if(it is JSResource) {
                val f = File(asm.root, it.path)
                if(f.exists() && f.canRead()) {
                    f.readLines().forEach {
                        log.debug(it)
                        resp.getWriter()?.append(it)
                        resp.getWriter()?.append("\n")
                    }
                }
            }
        }

        resp.getWriter()?.flush()
        resp.getWriter()?.close()
    }
}

class LoginServlet() : BosorkServlet() {
    protected override val methods: Set<String> = setOf("POST")


    public override fun init(config: ServletConfig?) {
        super<BosorkServlet>.init(config)
        //val app = config!!.getServletContext()!!.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule
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
                om.writerWithDefaultPrettyPrinter()!!.writeValue(resp.getWriter(), createResponse(bs.token))
                return
            }
        }

        val app = req.getServletContext()?.getAttribute(AppServletModule.APP_ATTRIBUTE) as AppServletModule

        val node = om.readTree(req.getReader())
        val user = node?.path("user")?.textValue()!!
        val password = node?.path("pwd")?.textValue()!!
        val cid = node?.path("cid")?.longValue()?:-1.toLong()
        val lreq = WebLoginRequest(req, cid, user, password)
        val loginResponse = app.app.login(lreq)
        if(loginResponse.error!=null) writeResponse(error(loginResponse.error!!), req, resp)
        writeResponse(ok(createResponse(loginResponse.session.token)), req, resp)
    }

    fun createResponse(token:URN): ObjectNode {
        val res = om.createObjectNode()
        res?.put("token", token.urn)
        return res!!
    }
}

class ServiceServlet(val service:URN) : BosorkServlet() {

    protected override fun serve(req: HttpServletRequest, resp: HttpServletResponse) {
        val session = req.getSession()
        if(session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE)==null)
            throw IllegalStateException("login before creating sockets")

        val bs = session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE) as BosorkServletSession
        logJetty.info("${service.urn} invoked")
        bs.request(Jsonifier.deserialise(req.getReader()!!, bs.app, bs.app.service(service)!!.request()) as BosorkRequest)
    }
    protected override val methods: Set<String> = setOf("PUT")
}

class WebLoginRequest(val req:HttpServletRequest, clientId:Long, user:String, pwd:String) : LoginRequest(clientId, user, pwd)

class ResponseSocket(session:HttpSession) : BosorkWebsocketAdapter(session) {
    public override fun onWebSocketConnect(sess: Session?) {
        logJetty.info("CONNECT ON SERVER")
        if(session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE)==null)
            throw IllegalStateException("login before creating sockets")

        val bs = session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE) as BosorkServletSession
        wssession = sess
        bs.responses = this
    }


    public override fun onWebSocketClose(statusCode: Int, reason: String?) {
        val bs = session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE) as BosorkServletSession
        bs.responses = null
    }


    public override fun onWebSocketText(message: String?) {

    }


}




class EventsSocket(session:HttpSession) : BosorkWebsocketAdapter(session) {
    public override fun onWebSocketConnect(sess: Session?) {
        if(session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE)==null)
            throw IllegalStateException("login before creating sockets")

        log.info("!!!!somebody wants EVENTS!!!!!")
        wssession = sess
        val bs = session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE) as BosorkServletSession
        bs.events = this
    }


    public override fun onWebSocketClose(statusCode: Int, reason: String?) {
        val bs = session?.getAttribute(BosorkServletSession.SESSION_ATTRIBUTE) as BosorkServletSession
        bs.events = null
    }


    public override fun onWebSocketText(message: String?) {

    }
}


class AppServletModule(val app:BosorkApp, val resources:Iterable<BosorkWebResource>, val root:File,
                       val workerResources:Iterable<BosorkWebResource>) {
    {
        app.listen(this)
    }



    fun init(ctx:ServletContext) {
        ctx.setAttribute(APP_ATTRIBUTE, this)
    }

    fun servlets(ctx:ServletContextHandler) {
        logJetty.info("adding ${javaClass<InitServlet>()} on path: ${app.id.specifier}")
        ctx.addServlet(javaClass<InitServlet>(), "/"+app.id.specifier)
        logJetty.info("adding ${javaClass<ResourceServlet>()} on path: ${"/"+app.id.specifier+"/resource/*"}")
        ctx.addServlet(javaClass<ResourceServlet>(), "/"+app.id.specifier+"/resource/*")
        logJetty.info("adding ${javaClass<LoginServlet>()} on path: ${"/"+app.id.specifier+"/login"}")
        ctx.addServlet(javaClass<LoginServlet>(), "/"+app.id.specifier+"/login")
        logJetty.info("adding ${javaClass<WebWorkerServlet>()} on path: ${"/"+app.id.specifier+"/webworker"}")
        ctx.addServlet(javaClass<WebWorkerServlet>(), "/"+app.id.specifier+"/webworker")

        app.services().forEach {
            val sh = ServletHolder(ServiceServlet(it.id))
            logJetty.info("adding ${it.id.urn} on path: ${"/"+app.id.specifier+"/${it.shortName}"}")
            ctx.addServlet(sh, "/"+app.id.specifier+"/${it.shortName}")
        }

        val l = object : HttpSessionListener {
            public override fun sessionCreated(se: HttpSessionEvent?) {
                logJetty.info("session created: ${se?.getSource()}")
            }
            public override fun sessionDestroyed(se: HttpSessionEvent?) {
                //val hs : HttpSession = se!!.getSource()!! as HttpSession

            }
        }
        ctx.getSessionHandler()!!.addEventListener(l)
    }

    fun sockets(ctx:ServletContextHandler) {
        ctx.socket(this.app) {
            Pair("/${app.id.specifier}/events", javaClass<EventsSocket>() as Class<BosorkWebsocketAdapter>)
        }

        ctx.socket(this.app) {
            Pair("/${app.id.specifier}/responses", javaClass<ResponseSocket>() as Class<BosorkWebsocketAdapter>)
        }
    }

    class object {
        val APP_ATTRIBUTE : String = "BOSORK_APP"
    }
}

fun<T> Iterator<T>.each(cb:(n:T)->Unit) {
    while (hasNext()) {
        cb(next())
    }
}

fun ArrayNode.each(cb:(n:JsonNode)->Unit) {
    this.iterator().each { cb(it) }
}

public fun readAppCfg(f:File) : AppCfg {
    val om = ObjectMapper()
    val root = om.readTree(java.io.FileReader(f))
    val title = root?.path("title")?.textValue()
    val port = root?.path("port")?.intValue()
    val appname = root?.path("app")?.textValue()

    val links = root?.path("links") as ArrayNode

    val resources = ArrayList<BosorkWebResource>()
    links.each {
        val href = it.path("href")?.textValue()
        val kind = it.path("type")?.textValue()
        val rel = it.path("rel")?.textValue()
        resources.add(LinkResource(href!!, "$appname/resource", kind!!, rel!!))
    }

    val scripts = root?.path("scripts") as ArrayNode

    scripts.each {
        val src = it.path("src")?.textValue()
        val kind = it.path("type")?.textValue()

        resources.add(JSResource(src!!, "$appname/resource"))
    }

    val workers = root?.path("worker") as ArrayNode

    val wresources = ArrayList<BosorkWebResource>()
    workers.each {
        val src = it.path("src")?.textValue()
        val kind = it.path("type")?.textValue()

        wresources.add(JSResource(src!!, "$appname/resource"))
    }

    return AppCfg(f.getParentFile()!!, appname!!, port!!, resources, wresources)
}

class AppCfg(val root: File, val appname:String, val port:Int, val resources:Iterable<BosorkWebResource>,val workerResources:Iterable<BosorkWebResource>) {
    var services : List<ServiceProvider> = ArrayList()
    var auth : AuthProvider = AnonymousAuthService.provider
}

public fun startApp(cfg:AppCfg) {
    val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", cfg.appname),
            cfg.services, DefaultWebAppSessionFactoryProvider(), cfg.auth)
    val asm = AppServletModule(app, cfg.resources, cfg.root, cfg.workerResources)
    val af = AppFactory(asm, cfg.port)
    af.init()
    af.server?.start()
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
                logJetty.info("!!!CL: ${ctx.getClassLoader()}")

                //http://www.javaintegrations.com/2012/08/using-embedded-jetty-with-guice-servlet.html
                ctx.addEventListener(object : ServletContextListener {
                    public override fun contextInitialized(ctx : ServletContextEvent?) {
                        logJetty.info("calling appmodule ${appmodule.app.id.urn} init")
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
                logJetty.info("calling appmodule ${appmodule.app.id.urn} servlets")
                appmodule.app.start()
                appmodule.servlets(ctx)
                appmodule.sockets(ctx)

                ctx
            }
        }

    }
}
