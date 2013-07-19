package ch.passenger.kotlin.html.js

import ch.passenger.kotlin.html.js.html.Observable
import ch.passenger.kotlin.html.js.model.Identifiable

/**
 * Created by sdju on 18.07.13.
 */
trait Interest : Observable<Identifiable> {
    val name : String
    
}