package p;

class A {
	public @interface Annotation {
		String name() default "foo";
	}
	private int bar() {
		return foo();
	}
	@Annotation (
		name= "bar"
	)
	public int foo() {
		return 2;
	}
}
class B extends A {
}