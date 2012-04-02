import test.ExternalClass;import java.util.List;import java.lang.annotation.ElementType;

abstract class SHTest {
	int field;
	static int staticField;
	final int finalField= 0;
	static final int staticFinalField= 0;
	void method(int param) {
		int local= param + 1;
		local++;
		staticMethod();
		abstractMethod();
		toString();
		for (int local2= 0; local2 < 10; local2++);
		try {} catch (Exception param2) {
			local= param2.hashCode();
			param2= null;
		}
	}
	static void staticMethod() {}
	abstract void abstractMethod();
	/** @deprecated */
	int deprecatedField;
	/** @deprecated */
	ExternalClass deprecatedMethod() {
		deprecatedField++;
		deprecatedMethod();
		return (InnerClass) null;
	}
	/** @deprecated */
	class InnerClass extends ExternalClass {
		SHTest parent= SHTest.this;
		int parentField= SHTest.this.field;
		int parentStaticField= SHTest.staticField;
		int parentFinalField= SHTest.this.finalField;
		int parentStaticFinalField= SHTest.staticFinalField;
	}

	@SuppressWarnings(value="all")
	class Generic<E extends Number> {
		E method() {
			List<String> list= null;
			SuppressWarnings annotationRef= null;
			ElementType enumRef= null;
			return null;
		}
	}
	static abstract class NestedSubClass extends SHTest {
		int inheritedField= field;
	}
}
