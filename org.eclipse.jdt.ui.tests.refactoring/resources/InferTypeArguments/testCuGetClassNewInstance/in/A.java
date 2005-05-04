package p;

class A {
	void doNothing() throws Exception {
		getClass().newInstance();
		this.getClass().newInstance();
		A newInstance= (A) getClass().newInstance();
		A test= (A) this.getClass().newInstance();
		
		RuntimeException rte= (RuntimeException) new ClassCastException().getClass().newInstance();
		ClassCastException cce= (ClassCastException) rte.getClass().newInstance();
	}
}