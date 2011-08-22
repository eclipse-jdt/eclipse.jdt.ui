package p;

class A {
	void m() {
		renamed();
	}

	void renamed() {}
}

class Ref {
	void bar(A a) {
		a.renamed();
	}
}
