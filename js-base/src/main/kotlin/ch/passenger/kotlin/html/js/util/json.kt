package ch.passenger.kotlin.html.js.util

/**
 * Created by Duric on 09.09.13.
 */
fun Json.getString(property:String):String {
    return get(property) as String
}

fun Json.getLong(property:String):Long {
    return get(property) as Long
}


fun<T> Json.getArray(property:String) : Array<T> {
    return get(property) as Array<T>
}

