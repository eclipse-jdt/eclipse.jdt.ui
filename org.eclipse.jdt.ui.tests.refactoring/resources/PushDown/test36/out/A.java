package p;

abstract class A {
}

class B extends A {
	public void m() {
	}
}

class C extends A {
	public void m() {
	}
}

class D extends C {
	@Override
	public void m() {
	}
}
