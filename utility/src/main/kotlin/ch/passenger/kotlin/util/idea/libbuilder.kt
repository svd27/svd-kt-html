package ch.passenger.kotlin.util.idea

/*
import org.vertx.java.core.Vertx
import org.vertx.java.core.VertxFactory
import org.vertx.java.platform.PlatformManager
import org.vertx.java.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vertx.java.core.buffer.Buffer
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.StringReader
import java.net.URLEncoder
import com.fasterxml.jackson.databind.node.ArrayNode
import java.util.ArrayList
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.ObjectNode
import javax.swing.JFrame
import javax.swing.JComponent
import java.awt.BorderLayout
import java.awt.Container
import javax.swing.JPanel
import kotlin.properties.Delegates
import org.vertx.java.core.AsyncResult
import org.vertx.java.platform.PlatformLocator
import org.vertx.java.platform.PlatformManagerFactory
import java.net.URL

/**
 * Created by sdju on 29.07.13.
 */
//private val log = LoggerFactory.getLogger(javaClass<RepoResolver>().getPackage()!!.getName())!!

val v = VertxFactory.newVertx()!!

fun dump(b : Buffer) {
    val om = ObjectMapper()

    val ow = om.writerWithDefaultPrettyPrinter()!!
    val s = b.toString("UTF-8")!!
    try {

        println(ow.writeValueAsString(om.readTree(StringReader(s))))
    } catch(e: Exception) {
        e.printStackTrace()
        println("error parsing: $s")
    }
}

fun main2(args : Array<String>) {
    val pfmf = PlatformLocator.factory
    val pm = pfmf?.createPlatformManager()

    val v = pm?.vertx()!!

    val client = v.createHttpClient()!!.setHost("search.maven.org")!!.setPort(80)!!
    /*
    val creq = client.connect("http://search.maven.org") {
        resp ->
        println("${resp?.statusMessage()}")
    }
    */
    /*
    """/solrsearch/select?q=g%3A%22io.vertx%22%20%20&rows=20&wt=json"""
    client.getNow("/remotecontent?filepath=activation/activation/1.0.2/activation-1.0.2.pom") {
        resp ->
        val sb = StringBuilder()
        resp!!.bodyHandler {
            b -> sb.append(b?.toString("UTF-8"))
            println("$sb")
        }
    }
    */


    val handler = object : Handler<Message<*>> {
        public override fun handle(msg: Message<*>?) {
            println("received: ${msg?.body()}")
            val om = ObjectMapper()
            val node = om.readTree(StringReader(msg?.body()!!.toString()))!!
            val docs = node.path("response")!!.path("docs")
            val r = ArrayList<RepoInfo>()
            if(docs is ArrayNode) {
                docs.forEach {
                    r.add(extract(it as ObjectNode))
                }
            }
            if(r.size > 0)
                r.first().deps()
        }
    }
    v.eventBus()?.registerHandler("repo.results", handler) {
        r -> println("Bus: ${r?.failed()}")
    }
    RepoResolver.searchNexus("dom4j", "dom4j", "ALL")

    /*
    frame("Search") {
        center {
            JPanel()
        }
    }
*/

    synchronized(v) {
        v.wait(60000)
    }

}

object RepoResolver {
    val vertx : Vertx = v
    fun searchNexus(group : String="", artifact:String="", version:String="ALL")  {
        assert(group.length()+artifact.length()>0)
        /*
        val template = StringBuilder("/solrsearch/select?q=")
        val il = template.length()
        val amp = '&'
        if(group.trim().length()>0) template.append("${if(template.length()>il) " AND " else ""}g%3A%22${group}%22%20%20")
        if(artifact.trim().length()>0) template.append("${if(template.length()>il) " AND " else ""}a%3A%22${artifact}%22%20%20")
        if(version.trim().length()>0) template.append("${if(template.length()>il) " AND " else ""}v%3A%22${version}%22%20%20")
        */
        val template = StringBuilder()
        val il = template.length()
        if(group.length()>0)
            template.append("g:\"$group\"")

        if(artifact.length()>0) {
            if(il!=template.length()) template.append("%20AND%20")
            template.append("a:\"$artifact\"")
        }

        if(version.length()>0) {
            if(version=="ALL")
                template.append("&core=gav")
            else {
                if(il!=template.length()) template.append("%20AND%20")
                template.append("v:\"$version\"")
            }

        }
        template.append("&wt=json")


        println("sending: ${template}")

        val client = v.createHttpClient()!!.setHost("search.maven.org")!!.setPort(80)!!
        val prefix = "/solrsearch/select?q="
        //val encode = URLEncoder.encode(template.toString(), "UTF-8")
        //println("encoded: $encode")
        client.getNow(prefix+template) {
            resp ->
            println("${resp?.statusCode()}: ${resp?.statusMessage()}")
            if(resp!!.statusCode() == 200)
                resp?.bodyHandler {
                    dump(it!!)
                    v.eventBus()!!.publish("repo.results", it.toString("UTF-8"))
                }
        }
    }
}

fun extract(it : ObjectNode) : RepoInfo {
    val ri = RepoInfo(it.path("id")?.asText()!!, it.path("g")?.asText()!!,
            it.path("a")?.asText()!!, it.path("v")?.asText()!!,
            it.path("p")?.asText()!!, it.path("timestamp")?.asLong())

    val pec = it.path("ec")
    if (!(pec is MissingNode)) {
        val ec = pec as ArrayNode
        ec.forEach {
            val s = it.textValue()
            if(s=="-javadoc.jar") {
                ri.docs = s
            }

            if(s=="-sources.jar") {
                ri.sources = s
            }
        }
    }

    return ri
}


class RepoInfo(val id:String, val group:String, val artifact:String, val version:String, val packaging:String, val ts:Long) {
    var docs : String? = null
    var sources : String? = null
    private var depsLoaded = false
    private val _realDeps : MutableList<RepoInfo> = ArrayList()


    fun filepath() : String {
        val sb = StringBuilder("remotecontent?filepath=")
        group.split('.').forEach { sb.append(it).append('/') }
        sb.append(version).append('/')
        sb.append(artifact).append('-').append(version).append("pom")
        return sb.toString()
    }

    fun deps() {
        if(!depsLoaded){
            depsLoaded = true
            val handler = object : Handler<Message<String>> {
                public override fun handle(msg: Message<String>?) {
                    println("pom: ${msg?.body()}")
                    val sax = org.dom4j.io.SAXReader()
                    val dom = sax.read(StringReader(msg?.body()!!))
                    val edl = dom?.selectNodes("project/dependencies")
                }
            }
            v.eventBus()?.registerHandler(id+".pom", handler)
            val client = v.createHttpClient()!!.setHost("search.maven.org")!!.setPort(80)!!
            println("file: ${filepath()}")
            client.getNow(filepath()) {
                resp ->

                if(resp?.statusCode()==200)
                    resp?.dataHandler {
                        b ->
                        val s = b?.toString("UTF-8")
                        v.eventBus()?.publish(id+".pom", s)
                    }
                else println("${resp?.statusCode()}: ${resp?.statusMessage()}")
            }
        }
    }
}
*/