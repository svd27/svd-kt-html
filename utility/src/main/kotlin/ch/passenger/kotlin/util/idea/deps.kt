package ch.passenger.kotlin.util.idea

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.ObjectNode
import sun.net.www.http.HttpClient
import java.net.URL
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import java.io.InputStreamReader
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URLEncoder
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.dom4j.io.SAXReader
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.AbstractAction
import java.awt.event.ActionEvent
import java.awt.FlowLayout
import javax.swing.JTable
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.util.ArrayList
import org.dom4j.Element
import java.util.regex.Pattern
import javax.swing.JComponent

/**
 * Created by sdju on 29.07.13.
 */

fun main(args: Array<String>) {
    //searchNexus("com.google.inject", "guice", "ALL")
    DepsUI().show()
}

fun searchNexus(group: String = "", artifact: String = "", version: String = "ALL", tm: SimpleTableModel<Artifact>) {
    assert(group.length() + artifact.length() > 0)

    val template = StringBuilder()
    val il = template.length()
    if(group.length() > 0 && group != "*")
        template.append("g%3A%22$group%22")

    if(artifact.length() > 0 && artifact != "*") {
        if(il != template.length()) template.append("%20AND%20")
        template.append("a%3A%22$artifact%22")
    }

    if(version.length() > 0) {
        if(version == "ALL")
            template.append("&core=gav")
        else {
            if(il != template.length()) template.append("%20AND%20")
            template.append("v%3A%22$version%22")
        }

    }
    template.append("&wt=json")
    val ex = "https://oss.sonatype.org/service/local/lucene/search?_dc=1375253977278&g=com.google.inject&a=guice&collapseresults=true"

    val prefix = "http://search.maven.org/solrsearch/select?q="
    //val query = prefix +URLEncoder.encode(template.toString(), "UTF-8")
    val query = prefix + template.toString()
    println("sending: ${query}")

    val client = DefaultHttpClient()
    val get = HttpGet(query)

    val response = client.execute(get)!!
    if(response.getStatusLine()?.getStatusCode() != 200) throw IllegalStateException(response.getStatusLine()?.getReasonPhrase())

    val sr = InputStreamReader(response.getEntity()!!.getContent()!!)
    val om = ObjectMapper()
    val node = om.readTree(sr)
    val nresp = node?.path("response")?.path("docs")
    if(!(nresp is MissingNode) && (nresp is ArrayNode))
        nresp.forEach {
            val a = artifact(it as ObjectNode)
            println(a.id)
            tm.add(a)
            //val pomin = getFile(a.pom())


            //val sax = SAXReader()
            //sax.read(pomin)

        }

}


fun getFile(path: String): InputStream {
    val client = DefaultHttpClient()
    println("http://search.maven.org/" + path)
    val get = HttpGet("http://search.maven.org/" + path)

    val response = client.execute(get)!!
    if(response.getStatusLine()?.getStatusCode() != 200) throw IllegalStateException(response.getStatusLine()?.getReasonPhrase())

    return  response.getEntity()!!.getContent()!!.buffered(1024)
}

fun artifact(it: ObjectNode): Artifact {
    println(it)
    var vn = it.path("v")
    if(vn is MissingNode) vn = it.path("latestVersion")
    val ri = Artifact(it.path("id")?.asText()!!, it.path("g")?.asText()!!,
            it.path("a")?.asText()!!, vn?.asText()!!,
            it.path("p")?.asText()!!, it.path("timestamp")?.asLong())

    val pec = it.path("ec")
    if (!(pec is MissingNode)) {
        val ec = pec as ArrayNode
        ec.forEach {
            val s = it.textValue()
            if(s == "-javadoc.jar") {
                ri.docs = s
            }

            if(s == "-sources.jar") {
                ri.sources = s
            }
        }
    }

    return ri
}

class Artifact(val id: String, val group: String, val artifact: String, val version: String, val packaging: String, val ts: Long): Comparable<Artifact> {
    var docs: String? = null
    var sources: String? = null
    var scope: String = "compile"
    var deps : List<Artifact>? = null

    fun pom(): String {
        val sb = StringBuilder("remotecontent?filepath=")
        group.split('.').forEach { sb.append(it).append('/') }
        sb.append(artifact).append('/')
        sb.append(version).append('/')
        sb.append(artifact).append('-').append(version).append(".pom")
        return sb.toString()
    }

    public fun equals(o: Any?): Boolean {
        return when(o) {
            o is Artifact -> id.equals((o as Artifact).id)
            else -> false
        }
    }


    public override fun compareTo(other: Artifact): Int {
        return id.compareToIgnoreCase(other.id)
    }

    public fun dependencies(): List<Artifact> {
        if(deps!=null) return deps!!
        val sax = SAXReader()
        val dom = sax.read(getFile(pom()))!!
        println(dom.asXML())
        val res = ArrayList<Artifact>()

        val xp = dom.selectNodes("//project/dependencies/dependency")
        val root = dom.getRootElement()
        val deps = root?.element("dependencies")
        val dep = deps?.elements("dependency")
        dep?.forEach {
            val n = it as Element
            println(it.asXML())
            val  g = n.elementText("groupId")!!
            val a = n.elementText("artifactId")!!
            var v = n.elementText("version")!!
            if(v.startsWith("\${")) {
                val pp = Pattern.compile("\\$\\{(.*)\\}")
                val m = pp.matcher(v)
                if(m.matches()) {
                    var pv = root?.element("properties")?.elementText(m.group(1))
                    if(pv != null) v = pv!!
                }
            }
            val s = n.elementText("scope")
            val el = Artifact("$g:$a:$v", g, a, v, "", 0)
            if(s != null) el.scope = s
            res.add(el)
        }

        this.deps = res
        return res
    }
}