package p;

class A {
	protected void m() {
		super.toString();
	}
}

class C extends A {
}

class B extends C {
}