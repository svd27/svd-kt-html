package ch.passenger.kotlin.html.js

import java.util.ArrayList

/**
 * Created by Duric on 31.08.13.
 */
fun<T> Iterable<T>.each(cb:(T)->Unit) {
    for(e in this) cb(e)
}

fun<T> Array<T>.each(cb:(T)->Unit) {
    for(e in this) cb(e)
}

fun<T> listOf(vararg ts :T): List<T> {
    val l = ArrayList<T>(ts.size)
    ts.each {
        l.add(it)
    }
    return l
}