package p;

class A {
	int f = 23;

	int foo() {
		int r = bar(f);
		int f = 43;
		return r;
	}
	
	private int bar(int x) {
		return x;
	}
}