package ch.passenger.kotlin.jetty

/**
 * Created by sdju on 20.08.13.
 */
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

class LinkResource(val path:String, val prefix:String, val kind:String, val rel:String) : BosorkWebResource {

    override fun createHeadTag(): String {
        return """
        <link rel="$rel" type="$kind" href="${prefix}/${path}">
        """
    }

    class object {
        fun css(path:String, prefix:String): LinkResource = LinkResource(path, prefix, "text/css", "stylesheet")
    }
}
