package p;

class A {
	private static void foo(int i, int j) {
		System.out.println(1+2);
		foo((1+2)*7, 1+2);
	}
}