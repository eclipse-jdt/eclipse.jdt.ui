package p;

abstract class A {
	public abstract int m(String[] bbb[][], int abb);
}
class B extends A {
	public int m(String[] bbb[][], int number) {
		return number + 0;
	}
}
class C extends B {
	public int m(String[] strings[][], int abb) {
		return abb + 17;
	}
}
