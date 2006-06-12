package p;
class A {
	void m() {
		final int a = 3;
		final int /*[*/xxx/*]*/ = 3;
		System.out.println(xxx);
		final int b = 3;
        System.out.println(b);
		final int b = 3;
		System.out.println(b);
	}
}
