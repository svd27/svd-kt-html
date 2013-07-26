package ch.passenger.kotlin.jetty

import org.junit.Test as test
import org.eclipse.jetty.server.nio.NetworkTrafficSelectChannelConnector
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.net.URL
import kotlin.test.assertEquals
import ch.passenger.kotlin.guice.injector
import org.eclipse.jetty.servlet.ServletContextHandler
import java.util.EnumSet
import javax.servlet.DispatcherType
import javax.servlet.http.HttpServlet
import com.google.inject.Singleton
import com.google.inject.servlet.ServletModule
import com.google.inject.Guice
import com.google.inject.Inject
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.servlet.DefaultServlet
import java.net.HttpURLConnection
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import java.util.EventListener
import javax.servlet.ServletContextListener
import javax.servlet.ServletContextEvent
import org.eclipse.jetty.server.Server
import ch.passenger.kotlin.jetty.jetty
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by sdju on 25.07.13.
 */


class EmbedderTest {
    test fun embed() {
        val greet = "Hans"
        val server = jetty() {
            connectors {
                val c0  = ServerConnector(this)

                c0.configure {
                    setPort(2709)
                }

                array(c0)
            }

            handlers {
                val h = object : AbstractHandler() {

                    public override fun handle(target: String?, req: Request?, httpReq: HttpServletRequest?, response: HttpServletResponse?) {
                        println("PI: ${req?.getPathInfo()}")
                        println("TARGET: ${target}")
                        response?.setContentType("text/html;charset=utf-8");
                        response?.setCharacterEncoding("utf-8")
                        response?.setStatus(HttpServletResponse.SC_OK);
                        req?.setHandled(true);

                        response?.getWriter()?.println("<h1>$greet</h1>");
                        //if (_body != null) response.getWriter().println(_body);
                    }
                }

                h
            }
        }

        try {
            server.start()

            val u  = URL("http://localhost:2709")
            u.openConnection()
            val s = u.readText("utf-8")
            val expect = "<h1>$greet</h1>"
            assertEquals(expect.toUpperCase(),s.toUpperCase())
        } finally {
            server.stop()
        }
    }


    test fun guice() {
        val server = jetty() {
            connectors {
                val c0  = ServerConnector(this)

                c0.configure {
                    setPort(2709)
                }

                array(c0)
            }

            handlers {
                val ctx = ServletContextHandler(ServletContextHandler.SESSIONS)
                ctx.setContextPath("/")
                ctx.addEventListener(object : ServletContextListener {
                    public override fun contextInitialized(p0: ServletContextEvent?) {
                        injector {
                            class SM [Inject]() : ServletModule() {
                                protected override fun configureServlets() {
                                    bind(javaClass<TestServlet>())!!.asEagerSingleton()
                                    serve("/*")!!.with(javaClass<TestServlet>())
                                }
                            }
                            + SM()

                        }
                    }
                    public override fun contextDestroyed(p0: ServletContextEvent?) {
                        println("${p0?.getSource()} destroyed")
                    }
                })

                //val dispatches = EnumSet.allOf(javaClass<DispatcherType>())
                val dispatches = EnumSet.of(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST)
                println(dispatches)
                val filterHolder = ctx.addFilter(javaClass<com.google.inject.servlet.GuiceFilter>(), "/*", dispatches)
                println(filterHolder)
                ctx.addServlet(javaClass<DefaultServlet>(), "/")

                ctx
            }

        }

        try {
            server.start()
            val client = DefaultHttpClient()
            val get = HttpGet("http://localhost:2709")
            val response = client.execute(get)
            println(response?.getEntity())
            val ins = response!!.getEntity()!!.getContent()
            val r = BufferedReader(InputStreamReader(ins!!))


            val sb = StringBuilder()
            r.forEachLine {
                sb.append(it)
            }
            /*
            var c  :Int = 0
            while(c>=0) {
                c = ins!!.read()
                if(c>=0) sb.append(c)
            }
            */

            println(sb.toString())
            assertEquals(TestServlet.textResp, sb.toString())

        } finally {
            server.stop()
        }

    }
}



