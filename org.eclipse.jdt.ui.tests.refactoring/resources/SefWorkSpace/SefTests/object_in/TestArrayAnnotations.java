package object_in;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE) @interface A {}
@Target(ElementType.TYPE_USE) @interface B {}
@Target(ElementType.TYPE_USE) @interface C {}
@Target(ElementType.TYPE_USE) @interface D {}
@Target(ElementType.TYPE_USE) @interface E {}

public class TestArrayAnnotations {
    boolean @D[]@E[] field@A[]@B []@C[];

	public TestArrayAnnotations() {
		field= new boolean[0][][][][];
	}
}
