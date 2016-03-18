package p;

interface SAM1 {
	X apply(X x1);
}

interface SAM2 {
	X apply(X x1);
}

interface SAM3 {
	X apply(X x1);
}

interface SAM4 {
	X apply(X x1);
}

class A {
	X map(X x) {
		return null;
	}
}

class B extends A {
	@Override
	X map(X x) {
		return null;
	}
}

class Util {

	static SAM1 lambda1 = (x) -> {
		return null;
	};

	static SAM2 lambda2 = x -> null;

	static SAM3 lambda3 = (X x) -> {
		return null;
	};

	static SAM4 lambda4 = (X x) -> null;

	static A a;

	void f1(X x1) {
		x1.m1();
	}

	void f2(X x2) { // only method that is NOT changed.
		x2.m2();
	}

	void f_via_A(X x1) {
		a.map(x1).m2();
	}

	void f_via_lambda1(X x1) {
		lambda1.apply(x1).m2();
	}

	void f_via_lambda2(X x1) {
		lambda2.apply(x1).m2();
	}

	void f_via_lambda3(X x1) {
		lambda3.apply(x1).m2();
	}

	void f_via_lambda4(X x1) {
		lambda4.apply(x1).m2();
	}

}
