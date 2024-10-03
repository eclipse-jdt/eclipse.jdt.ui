package p;

class A {
	int f;

	void mA() {
		class B extends A {
			public void mB() {
				f = 0;
			}
		}
	}
}
