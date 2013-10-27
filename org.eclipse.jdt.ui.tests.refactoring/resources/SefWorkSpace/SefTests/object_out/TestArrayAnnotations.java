package object_out;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE) @interface A {}
@Target(ElementType.TYPE_USE) @interface B {}
@Target(ElementType.TYPE_USE) @interface C {}
@Target(ElementType.TYPE_USE) @interface D {}
@Target(ElementType.TYPE_USE) @interface E {}

public class TestArrayAnnotations {
    private boolean @D[]@E[] field@A[]@B []@C[];

	public TestArrayAnnotations() {
		setField(new boolean[0][][][][]);
	}

	boolean@A[]@B []@C[]@D[]@E[] getField() {
		return field;
	}

	void setField(boolean @D[]@E[] field@A[]@B []@C[]) {
		this.field = field;
	}
}
