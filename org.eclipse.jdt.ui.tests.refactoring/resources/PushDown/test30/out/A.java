package p;

abstract class A {
	public @interface Annotation {
		String name() default "foo";
	}
	private int bar() {
		return foo();
	}
	@Annotation (
		name= "bar"
	)
	public abstract int foo();
}
class B extends A {

	@Annotation(name = "bar")
	@Override
	public int foo() {
		return 2;
	}
}