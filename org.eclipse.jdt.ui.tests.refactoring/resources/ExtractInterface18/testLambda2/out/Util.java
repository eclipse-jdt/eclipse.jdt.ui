package p;

interface SAM1 {
	X apply(I x1);
}

interface SAM2 {
	X apply(I x1);
}

interface SAM3 {
	X apply(I x1);
}

interface SAM4 {
	X apply(I x1);
}

class A {
	X map(I x) {
		return null;
	}
}

class B extends A {
	@Override
	X map(I x) {
		return null;
	}
}

class Util {

	static SAM1 lambda1 = (x) -> {
		return null;
	};

	static SAM2 lambda2 = x -> null;

	static SAM3 lambda3 = (I x) -> {
		return null;
	};

	static SAM4 lambda4 = (I x) -> null;

	static A a;

	void f1(I x1) {
		x1.m1();
	}

	void f2(X x2) { // only method that is NOT changed.
		x2.m2();
	}

	void f_via_A(I x1) {
		a.map(x1).m2();
	}

	void f_via_lambda1(I x1) {
		lambda1.apply(x1).m2();
	}

	void f_via_lambda2(I x1) {
		lambda2.apply(x1).m2();
	}

	void f_via_lambda3(I x1) {
		lambda3.apply(x1).m2();
	}

	void f_via_lambda4(I x1) {
		lambda4.apply(x1).m2();
	}

}
