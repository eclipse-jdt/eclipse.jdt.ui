package p;

class A {
	int f;

	void m() {
	}

	void mA() {
		class B extends A {
			public void mB() {
				super.m();
				super.f = 0;
			}
		}
	}
}
