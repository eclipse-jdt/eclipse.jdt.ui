package p; // 4, 16, 4, 23
class A {
	void foo(C o) {
		int i= o.get();
		int m= i;
		int n= i;
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