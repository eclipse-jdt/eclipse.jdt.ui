package p;

abstract class A {
	public abstract int m(int a, String[] b[][]);
}
class B extends A {
	public int m(int number, String[] b[][]) {
		return number + 0;
	}
}
class C extends B {
	public int m(int a, String[] strings[][]) {
		return a + 17;
	}
}
