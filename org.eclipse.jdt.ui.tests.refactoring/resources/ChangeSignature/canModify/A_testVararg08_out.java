package p;

class A {
	/**
	 * This is {@link #m(String...)}.
	 * @param args TODO
	 */
	public void m(String... args) {
		if (12 > 12)
			m(args);
	}
	void x() {
		m();
	}
}

class B {
	public void b() {
		new A().m();
	}	
}
