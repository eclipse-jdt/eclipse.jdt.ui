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
		return x;
	}
}

class Util {

	static SAM1 lambda1 = (x) -> {
		return x;
	};

	static SAM2 lambda2 = x -> x;

	static SAM3 lambda3 = (X x) -> {
		return x;
	};

	static SAM4 lambda4 = (X x) -> x;

	static A a;

	void f1(I x1) { // only change here
		x1.m1();
	}

	void f2(X x2) {
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
