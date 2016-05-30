package p;

class X {
	String value;
	X field = this;

	public X(String value) {
		this.value = value;
	}

	public String m1() {
		return value;
	}

	public String m2() {
		return value;
	}

	public X(X otherX) {
		this.value = "created in X::new";
	}

	public X instanceMethod() {
		return new X("created in X::instanceMethod");
	}

	public X instanceMethod2(X x) {
		return new X("created in X::instanceMethod2");
	}

	public static X staticMethod(X x) {
		return new X("created in X::staticMethod");
	}
}