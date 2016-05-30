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
		this.value = "copy of " + otherX.value;
	}

	public X instanceMethod() {
		return this;
	}

	public X instanceMethod2(X x) {
		return x;
	}

	public static X staticMethod(X x) {
		return x;
	}
}