package p;

import java.lang.annotation.Annotation;

@interface A {
	String value() default "default";
}
class B implements A {
    public void m() {
        System.out.println("public method");
    }
    @Override
    public String value() {
        return "";
    }
    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
