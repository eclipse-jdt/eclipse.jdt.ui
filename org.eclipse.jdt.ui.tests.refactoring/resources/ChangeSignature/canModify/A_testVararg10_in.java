package p;

class A {
	/**
	 * This is {@link #m()}.
	 */
	public void m() {
		if (12 > 12)
			m();
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
