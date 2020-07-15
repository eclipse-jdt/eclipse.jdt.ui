package p;

abstract class A {
	abstract void m();
}

class B extends A {
	@Override
	public void m() {
	}
}

class C extends A {
	@Override
	public void m() {
	}
}

class D extends C {
	@Override
	public void m() {
	}
}
