package p;
abstract class A {

	abstract void m();

}

abstract class B extends A {
}

class C extends A {

	protected void m() {
	}

}
