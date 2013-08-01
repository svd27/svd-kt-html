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
import java.io.File
import java.io.FileOutputStream
import org.dom4j.DocumentFactory
import org.dom4j.io.XMLWriter
import java.io.FileWriter
import org.dom4j.Document
import java.io.FileReader
import java.util.HashMap
import java.io.FileNotFoundException

/**
 * Created by sdju on 29.07.13.
 */

fun main(args: Array<String>) {
    //searchNexus("com.google.inject", "guice", "ALL")
    DepsUI().show()
}

fun searchNexus(group: String = "", artifact: String = "", version: String = "ALL"): List<Artifact> {
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
    template.append("&wt=json").append("&rows=100")
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
    val res = ArrayList<Artifact>()
    if(!(nresp is MissingNode) && (nresp is ArrayNode))
        nresp.forEach {
            val a = artifact(it as ObjectNode)
            println(a.id)
            res.add(a)
            //val pomin = getFile(a.pom())


            //val sax = SAXReader()
            //sax.read(pomin)

        }
    return res
}

val artifactCache : MutableMap<String,Artifact> = object : HashMap<String,Artifact>() {

}


fun getFile(path: String): InputStream {
    val client = DefaultHttpClient()
    println("http://search.maven.org/" + path)
    val get = HttpGet("http://search.maven.org/" + path)

    val response = client.execute(get)!!
    if(response.getStatusLine()?.getStatusCode() != 200) throw FileNotFoundException(path +": " + response.getStatusLine()?.getReasonPhrase())

    return  response.getEntity()!!.getContent()!!.buffered(1024)
}

fun artifact(it: ObjectNode): Artifact {
    println(it)
    var vn = it.path("v")
    if(vn is MissingNode) vn = it.path("latestVersion")
    val id = it.path("id")?.asText()!!
    if(artifactCache.containsKey(id)) return artifactCache[id]!!
    val ri = Artifact(id, it.path("g")?.asText()!!,
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

public fun readCfg(f:File) : LibCfg {
    val fr = FileReader(f)
    val om = ObjectMapper()
    val node = om.readTree(fr)!! as ObjectNode

    val leaderId = node.path("name")!!.textValue()!!
    val libDir = node.path("libdir")!!.textValue()!!

    val aa = node.path("artifacts")!! as ArrayNode

    var leader :Artifact? = null
    val res = ArrayList<Artifact>()
    aa.forEach {
        val n = it!! as ObjectNode
        val aid = n.path("id")!!.textValue()!!
        val group = n.path("group")!!.textValue()!!
        val version = n.path("version")!!.textValue()!!
        val artifact = n.path("artifact")!!.textValue()!!
        val list = searchNexus(group, artifact, version)
        if(list.size()>0) {
            res.add(list.first())
            if(list.first().id==leaderId) leader = list.first()
        }
    }

    if(leader==null) {
        leader = res.first()
    }
    return LibCfg(leader!!, res, libDir)
}

public fun saveCfg(f:File, cfg:LibCfg) {
    val om = ObjectMapper()
    val sv = om.writerWithDefaultPrettyPrinter()!!.writeValueAsString(cfg.asJson())!!
    val fw = FileWriter(f)
    try {
        fw.write(sv)
        fw.flush()
    } finally {
        fw.close()
    }

}

class LibCfg(val leader:Artifact, val artifacts:List<Artifact>, val libDir:String) {
    public fun asJson() : ObjectNode {
        val om = ObjectMapper()
        val root = om.createObjectNode()!!
        root.put("name", leader.id)
        root.put("libdir", libDir)
        val an = om.createArrayNode()!!
        root.put("artifacts", an)
        artifacts.forEach {
            val node = om.createObjectNode()!!
            an.add(node)
            node.put("id", it.id)
            node.put("group", it.group)
            node.put("version", it.version)
            node.put("artifact", it.artifact)
            node.put("packaging", it.packaging)
        }
        return root
    }
}


public fun pack(cfg:LibCfg, projectDir:String) {
    val leader = cfg.leader
    val artifacts = cfg.artifacts
    var libDir = cfg.libDir
    var idlibs = File(projectDir+"/.idea/libraries")
    if(!idlibs.exists()) {
        throw IllegalStateException()
    }
    val doc = DocumentFactory.getInstance()?.createDocument()!!
    doc.addElement("component")
    val root = doc.getRootElement()!!
    root.addAttribute("name", "libraryTable")
    val lib = root.addElement("library")?.addAttribute("name", leader.id)
    lib?.addElement("properties")?.addAttribute("maven-id", leader.id)
    val ecl = lib?.addElement("CLASSES")!!
    val ejd = lib?.addElement("JAVADOC")!!
    val esrc = lib?.addElement("SOURCES")!!
    if(!libDir.startsWith("/")) libDir = "/"+libDir
    artifacts.forEach {
        try {
            it.download(libDir)
            //<root url="jar://$PROJECT_DIR$/lib/vertx-platform-2.0.0-final.jar!/" />
            val jardir = "${it.group}/${it.version}/${it.artifact}"
            ecl.addElement("root")?.addAttribute("url", "jar://\$PROJECT_DIR\$$libDir/${jardir}/${it.jarName()}!/")
            if(it.docs!=null) {
                ejd.addElement("root")?.addAttribute("url", "jar://\$PROJECT_DIR\$$libDir/${jardir}/${it.artifact}-${it.version}${it.docs}!/")
            }
            if(it.sources!=null) {
                esrc.addElement("root")?.addAttribute("url", "jar://\$PROJECT_DIR\$$libDir/${jardir}/${it.artifact}-${it.version}${it.sources}!/")
            }
        } catch(e: FileNotFoundException) {
            e.printStackTrace()
        }
    }
    val w = XMLWriter()
    try {
        val fn = leader.id.replace('.', '_').replace(':', '_')
        val dest = File(idlibs, fn + ".xml")
        println("Writing to...${dest}")
        val fw = FileWriter(dest)
        w.setWriter(fw)
        w.write(doc)
        w.flush()
    } finally {
        w.close()
    }

}


class Artifact(val id: String, val group: String, val artifact: String, val version: String, val packaging: String, val ts: Long): Comparable<Artifact> {
    var docs: String? = null
    var sources: String? = null
    var deps : List<Artifact>? = null
    var scope : String = ""

    fun jarName() : String {
        return "$artifact-$version.jar"
    }

    fun pomFile(): String {
        val sb = StringBuilder("remotecontent?filepath=")
        group.split('.').forEach { sb.append(it).append('/') }
        sb.append(artifact).append('/')
        sb.append(version).append('/')
        sb.append(artifact).append('-').append(version).append(".pom")
        return sb.toString()
    }

    fun file(ext:String) :String {
        val sb = StringBuilder("remotecontent?filepath=")
        group.split('.').forEach { sb.append(it).append('/') }
        sb.append(artifact).append('/')
        sb.append(version).append('/')
        sb.append(artifact).append('-').append(version).append(ext)
        return sb.toString()
    }

    public fun equals(o: Any?): Boolean {
        return when(o) {
            o is Artifact -> id.equals((o as Artifact).id)
            else -> false
        }
    }

    public fun download(dest : String) : String {
        val root = File(dest)
        val gf = File(root, group)
        val vf = File(gf, version)
        val dest = File(vf, artifact)

        dest.mkdirs()
        val jn = file(".jar")
        save(dest, "$artifact-$version.jar", ".jar")
        if(sources!=null)
            save(dest, "$artifact-$version$sources", "$sources")
        if(docs!=null)
            save(dest, "$artifact-$version$docs", "$docs")

        return "$group/$version"
    }

    public fun save(dir:File, fn:String, ext:String) {
        val target = File(dir, fn)
        println("write file '${target.getAbsoluteFile()}'")
        if(target.exists()) {
            return
        }
        println(file(ext))
        val io = getFile(file(ext))
        val fout = FileOutputStream(target)
        try {
            val buffer = ByteArray(1024)
            var len = io?.read(buffer)
            while (len != -1) {
                fout.write(buffer, 0, len);
                len = io?.read(buffer);
                if (Thread.interrupted()) {
                    throw InterruptedException();
                }
            }
            fout.flush()
        } finally {
            fout.close()
        }
    }


    public override fun compareTo(other: Artifact): Int {
        return id.compareToIgnoreCase(other.id)
    }

    private val NOTLOADED : Element = DocumentFactory.getInstance()?.createElement("!!!xxx!!!")!!
    var dm : Element? = NOTLOADED
    val POMNOTLOADED = DocumentFactory.getInstance()!!.createDocument()!!
    var pom : Document = POMNOTLOADED
    var PNOTLOADED = true
    var parent : Artifact? = null

    fun getPOMDoc() : Document {
        if(pom.identityEquals(POMNOTLOADED)) {
            pom = SAXReader().read(getFile(pomFile()))!!
        }
        return pom
    }

    fun getParentArtifact() :Artifact? {
        if(!PNOTLOADED) return parent
        PNOTLOADED=true
        parent = null
        val idparent = parentId(getPOMDoc())
        if(idparent !=null) {
            var canSearch :Boolean = true
            idparent.forEach { if(it==null) canSearch = false }
            if (canSearch) {
                val l = searchNexus(idparent[0]!!, idparent[2]!!, idparent[1]!!)
                if(l.size()>0) {
                    parent = l.first()
                }
            }
        }
        return parent
    }

    fun getDepManagment() : Element? {
        if(dm == NOTLOADED) {
            val root = getPOMDoc().getRootElement()
            if(root?.element("dependencyManagement") != null) {
                dm = root?.element("dependencyManagement")!!
            } else {
                dm = getParentArtifact()?.getDepManagment()
            }
        }

        return dm
    }

    public fun parentName() :String{
        val pid = parentId(getPOMDoc())
        if(pid!=null && pid.size==3) {
            return "${pid[0]}.${pid[1]}.${pid[2]}"
        }

        return "<root>"
    }

    private fun parentId(doc: Document) :Array<String?>? {
        val pe = doc.getRootElement()?.element("parent")
        if(pe==null) return null
        val g = pe?.elementText("groupId")
        var v = pe?.elementText("version")
        val a = pe?.elementText("artifactId")
        if(v==null || v?.startsWith("\${")?:true)  {
            //assume parent has same version as child
            v = version
        }
        return array(g, v, a)
    }


    private fun resolveVersion(g:String, a:String) : String {
        var v = "unknown"
        println("looking for v of $g.$a in $id")
        //9.0.4.v20130625

        val dm = getDepManagment()

        if(dm!=null) {
            dm.element("dependencies")?.elements("dependency")?.forEach {
                val e = it as Element
                val dg = e.elementText("groupId")
                val da = e.elementText("artifactId")
                println("comparing a: '$a' == '$da'")
                println("comparing g: '$g' == '$dg'")
                if(v=="unknown" && g== dg && a== da) {
                    println("matched ${e.elementText("version")}")
                    v = e.elementText("version")!!
                }
            }
        }
        if(v=="unknown") v = getParentArtifact()?.resolveVersion(g, a)?:"unknown"

        return v
    }

    public fun dependencies(): List<Artifact> {
        if(deps!=null) return deps!!
        val sax = SAXReader()
        val dom = sax.read(getFile(pomFile()))!!
        println(dom.asXML())
        val res = ArrayList<Artifact>()

        val root = dom.getRootElement()
        val deps = root?.element("dependencies")
        val dep = deps?.elements("dependency")

        dep?.forEach {
            val n = it as Element
            println(it.asXML())
            val  g = n.elementText("groupId")!!
            val a = n.elementText("artifactId")!!
            var v = n.elementText("version")
            if(v==null) {
                v = resolveVersion(g, a)
            }

            if(v!!.startsWith("\${")) {
                v = resolveProperty(v!!)
            }
            val s = n.elementText("scope")
            val aid = "$g:$a:$v"
            val el =
            if(artifactCache.containsKey(aid)) artifactCache[aid]!!
            else Artifact(aid, g, a, v!!, "", 0)
            if(s!=null) el.scope = s

            res.add(el)
        }

        this.deps = res
        return res
    }

    fun resolveProperty(v:String) : String {
        println("looking for property: $v in $id")
        val pp = Pattern.compile("\\$\\{(.*)\\}")
        val m = pp.matcher(v)
        var res :String = v
        if(m.matches()) {
            if(m.group(1)=="project.version") return version
            var pv = getPOMDoc().getRootElement()?.element("properties")?.elementText(m.group(1))
            if(pv != null) res = pv!!
            else {
                println("continue resolve in: ${getParentArtifact()?.id}")
                res = getParentArtifact()?.resolveProperty(v)?:v
            }
        }
        return res
    }
}