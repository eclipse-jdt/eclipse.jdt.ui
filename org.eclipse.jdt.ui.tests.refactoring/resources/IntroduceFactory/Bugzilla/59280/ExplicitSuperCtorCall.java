package p;

public class ExplicitSuperCtorCall {
	public /*[*/ExplicitSuperCtorCall/*]*/() {
	}
}

class B extends ExplicitSuperCtorCall {
	public B() {
		super();
	}
}
