package p;

public class UpdateSimpleName {
	private int foo;
	private int foo2;
	public void foo() {
		foo=foo2;
		this.foo=foo2;
		foo=this.foo2;
		foo=Math.abs(foo);
		UpdateSimpleName usn= new UpdateSimpleName();
		usn.foo= usn.foo2;
		usn.foo= foo;
		usn.foo2++;
	}
}
