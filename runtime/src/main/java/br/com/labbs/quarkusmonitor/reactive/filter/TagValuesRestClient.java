package br.com.labbs.quarkusmonitor.reactive.filter;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TagValuesRestClient {
    String name() default "";
    String address() default "";
}
