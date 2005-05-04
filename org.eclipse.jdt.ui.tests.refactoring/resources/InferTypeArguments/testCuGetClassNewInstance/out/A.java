package p;

class A {
	void doNothing() throws Exception {
		getClass().newInstance();
		this.getClass().newInstance();
		A newInstance= getClass().newInstance();
		A test= this.getClass().newInstance();
		
		RuntimeException rte= new ClassCastException().getClass().newInstance();
		ClassCastException cce= (ClassCastException) rte.getClass().newInstance();
	}
}