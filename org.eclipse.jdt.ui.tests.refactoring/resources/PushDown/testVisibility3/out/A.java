package p;

class A {
	static class T{}
}
class B extends A {

	public T f() {
		return new T();
	}
}