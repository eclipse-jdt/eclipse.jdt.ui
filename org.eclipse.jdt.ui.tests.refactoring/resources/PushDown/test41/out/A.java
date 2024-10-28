package p;

class A {
	void mA() {
		class B extends A {
			int f;

			public void mB() {
				m();
				this.f = 0;
			}

			void m() {
			}
		}
	}
}
