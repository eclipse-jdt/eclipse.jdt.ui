package p;

class A {
}

class C extends A {
}

class B extends C {
	void m() {
		super.toString();
	}
}