package p;

class A {
	private static class T{}
	public T f(){
		return new T();
	}
}
class B extends A {
}