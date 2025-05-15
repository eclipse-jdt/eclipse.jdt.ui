package p3;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import p1.MovingClass;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface MyAnnotation {
}

public class TestMoving {

	public List<@MyAnnotation MovingClass> list;

}