package ch.passenger.kotlin.html.js.html.svg

/**
 * Created by svd on 22/08/13.
 */

native trait SVGElement {
    native var id: String?
    native var xmlbase: String?
    native val ownerSVGElement: SVGSVGElement?
    native val viewportElement: SVGElement?
}

native trait SVGLocatable {
    native val nearestViewportElement: SVGElement ;
    native val  farthestViewportElement: SVGElement ;

    native fun getBBox(): SVGRect
    native fun getCTM(): SVGMatrix
    native fun getScreenCTM(): SVGMatrix
    native fun getTransformToElement(element: SVGElement): SVGMatrix
}

native trait SVGSVGElement : SVGElement {

}

native trait SVGRect {
    native var x: Double
    native var y :Double
    native var width :Double
    native var height :Double
}
native trait SVGMatrix {
    native var a: Double
    native var b: Double
    native var c: Double
    native var d: Double
    native var e: Double
    native var f: Double

    fun multiply(m: SVGMatrix): SVGMatrix
    fun inverse(m: SVGMatrix): SVGMatrix
    fun translate(x: Double, y: Double): SVGMatrix
    fun scale(s: Double): SVGMatrix
    fun scaleNonUniform(sx: Double, sy: Double): SVGMatrix
    fun rotate(angle: Double): SVGMatrix
    fun rotateFromVector(x: Double, y: Double): SVGMatrix
    fun flipX(): SVGMatrix
    fun flipY(): SVGMatrix
    fun skewX(x: Double): SVGMatrix
    fun skewY(y: Double): SVGMatrix
}

native trait SVGPoint {
    var x: Double
    var y: Double

    fun matrixTransform(matrix: SVGMatrix): SVGPoint
}