package p;

public class ExplicitSuperCtorCall {
	public static ExplicitSuperCtorCall createExplicitSuperCtorCall() {
		return new ExplicitSuperCtorCall();
	}

	protected /*[*/ExplicitSuperCtorCall/*]*/() {
	}
}

class B extends ExplicitSuperCtorCall {
	public B() {
		super();
	}
}
