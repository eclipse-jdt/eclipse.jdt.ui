package p; // 4, 16, 4, 23
class A {
	void foo(C o) {
		int m= o.get();
		int n= o.get();
	}
}

class B extends C {
	D d1;
	int get() {
		return d1.v;
	}
}

class C {
	D d2;

	int get() {
		return d2.v;
	}
}

class D {
	int v;
}