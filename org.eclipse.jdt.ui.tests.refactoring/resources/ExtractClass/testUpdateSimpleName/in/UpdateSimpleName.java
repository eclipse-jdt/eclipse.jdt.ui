package p;

public class UpdateSimpleName {
	private int foo;
	private int foo2;
	public void foo() {
		foo=foo2;
		this.foo=foo2;
		foo=this.foo2;
		foo=Math.abs(foo);
	}
}
