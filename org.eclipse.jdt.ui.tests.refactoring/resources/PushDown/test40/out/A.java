package p;

class A {
	void mA() {
		class B extends A {
			int f;

			public void mB() {
				f = 0;
			}
		}
	}
}
