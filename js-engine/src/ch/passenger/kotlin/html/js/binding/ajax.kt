package ch.passenger.kotlin.html.js.binding

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 04.07.13
 * Time: 18:46
 */

native public public class XMLHttpRequest() {
    public fun open(method : String, url : String, async : Boolean = true, user : String? = null, password : String? = null  ) : Unit = js.noImpl

    public native var onreadystatechange : () -> Unit = js.noImpl
    public native var responseText : String = js.noImpl
    public native var readyState : Short = js.noImpl
    public native fun send() : Unit = js.noImpl
}
