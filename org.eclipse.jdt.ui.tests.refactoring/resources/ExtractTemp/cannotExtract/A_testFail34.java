package p;

class A {
	int f(int i) {
		return i;
	}

	int f1(int is) {
		for (f(9), f(8);;) {
		}
	}
}
