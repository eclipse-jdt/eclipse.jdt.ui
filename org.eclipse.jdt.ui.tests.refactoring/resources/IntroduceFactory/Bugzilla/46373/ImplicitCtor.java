package p;

public class ImplicitCtor {
	public void foo() {
		new ImplicitCtor();
	}
	public void bar() {
		new /*[*/ImplicitCtor/*]*/();
	}
}
