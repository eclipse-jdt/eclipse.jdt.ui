package p;

class A {
	private static void foo(int i, int j) {
		int temp = 1+2;
		System.out.println(temp);
		foo(temp*7, temp);
	}
}