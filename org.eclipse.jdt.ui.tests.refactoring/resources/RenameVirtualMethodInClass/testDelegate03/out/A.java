package p;

public abstract class A {
	abstract void k();

	/**
	 * @deprecated Use {@link #k()} instead
	 */
	abstract void m();
}

class B extends A {
	void k() {
		//Foo
	}

	/**
	 * @deprecated Use {@link #k()} instead
	 */
	void m() {
		k();
	}
}

class C extends B {
	void k() {
		//Bar
		C c= new C() {
			void k() {
				// X
			}

			/**
			 * @deprecated Use {@link #k()} instead
			 */
			void m() {
				k();
			}
		};
	}

	/**
	 * @deprecated Use {@link #k()} instead
	 */
	void m() {
		k();
	}
}
