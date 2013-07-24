package ch.passenger.test;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by sdju on 24.07.13.
 */

@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Echo {
}
