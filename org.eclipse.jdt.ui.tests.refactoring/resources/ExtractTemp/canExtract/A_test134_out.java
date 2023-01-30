package p; // 4, 29, 4, 45
class A {
	void foo(B b) {
		int i= b.getThenInc();
		System.out.println( i + (b.getThenInc()));
	}
}

class B extends C {

}

class C implements I {
	D d;

	int getThenInc() {
		return (((this).d).v)++;
	}
}

class D {
	int v;
}

interface I {
	int getThenInc();
}