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

class BosorkServletSession(val servletSession : HttpSession, override val app : BosorkApp, override val token: URN) : BosorkSession {
    override val attributes: MutableMap<String, Any?> = HashMap()
    override val reqBus: MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus: MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
    override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap();

    {
        servletSession.setAttribute(SESSION_ATTRIBUTE, this)
    }

    class object {
        val SESSION_ATTRIBUTE :String = "BOSORK-SESSION"
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

class InitServlet : HttpServlet() {
    protected override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        when(req?.getMethod()) {
            "GET" -> { serve(req!!, resp!!)}
            else -> super.service(req, resp)
        }
    }

    private fun serve(req: HttpServletRequest, resp: HttpServletResponse) {
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

class ResourceServlet : HttpServlet() {

    protected override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        when(req?.getMethod()) {
            "GET" -> {resource(req!!,resp!!)}
            else -> super<HttpServlet>.service(req, resp)
        }
    }

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




class AppServletModule(val app:BosorkApp, val resources:Array<BosorkWebResource>, val root:File) {
    fun init(ctx:ServletContext) {
        ctx.setAttribute(APP_ATTRIBUTE, this)
    }

    fun servlets(ctx:ServletContextHandler) {
        log.info("adding ${javaClass<InitServlet>()} on path: ${app.id.specifier}")
        ctx.addServlet(javaClass<InitServlet>(), "/"+app.id.specifier)
        log.info("adding ${javaClass<ResourceServlet>()} on path: ${"/"+app.id.specifier+"/resource"}")
        ctx.addServlet(javaClass<ResourceServlet>(), "/"+app.id.specifier+"/resource/*")
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
                appmodule.servlets(ctx)

                ctx
            }
        }

    }
}

fun main(args:Array<String>) {
    val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"), ArrayList())
    val wapp = AppServletModule(app, array(JSResource("resources/js/jquery-1.7.2.js", "test/resource"), CSSResource("resources/html/base.css", "test/resource")), File("D:/dev/svd/proj/kotlin/svd-kt-html/Container/web"))
    val waf = AppFactory(wapp, 2709)
    waf.init()
    waf.server?.start()
}