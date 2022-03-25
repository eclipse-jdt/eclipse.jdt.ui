package p;

class A {
	int f = 23;

	int foo() {
		int f = 43;
		return bar(this.f);
	}
	
	private int bar(int x) {
		return x;
	}
}