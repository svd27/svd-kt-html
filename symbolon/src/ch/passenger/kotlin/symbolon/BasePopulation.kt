package ch.passenger.kotlin.symbolon

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 10:55
 * To change this template use File | Settings | File Templates.
 */

object LetterType : TypeWord("Letter", Universe.id())
object LanguageType : TypeWord("Language", Universe.id())
object ColorType : TypeWord("Color", Universe.id())

object Populator {
    fun populate() {
        Universe.add(LetterType)
        Universe.add(LanguageType)
        Universe.add(ColorType)
        val latin = Word("Latin", Universe.id())
        Universe.add(latin)
        for(l in 'a'..'z') {
            val w = Word("" + l, Universe.id())
            w.kind = LetterType
            w.qualities.add(latin)
            Universe.add(w)
        }
    }
}