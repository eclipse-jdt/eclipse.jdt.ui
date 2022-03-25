package p;

public class A {
	static int f= 42;

	int foo() {
		int f= 23;
		return A.f;
	}
}
