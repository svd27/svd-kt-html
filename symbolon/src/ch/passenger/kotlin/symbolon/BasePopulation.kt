package ch.passenger.kotlin.symbolon

import ch.passenger.kotlin.basis.URN

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 10:55
 * To change this template use File | Settings | File Templates.
 */

object LetterType : TypeWord("Letter", URN(URN.word()))
object LanguageType : TypeWord("Language", URN(URN.word()))
object ColorType : TypeWord("Color", URN(URN.word()))

object Populator {
    fun populate() {
        Universe.add(LetterType)
        Universe.add(LanguageType)
        Universe.add(ColorType)
        val latin = Word("Latin", URN(URN.word()))
        Universe.add(latin)
        for(l in 'a'..'z') {
            val w = Word("" + l, URN(URN.word()))
            w.kind = LetterType
            w.qualities.add(latin)
            Universe.add(w)
        }
    }
}