package p;

class A {
	protected static class T{}
}
class B extends A {

	public T f() {
		return new T();
	}
}