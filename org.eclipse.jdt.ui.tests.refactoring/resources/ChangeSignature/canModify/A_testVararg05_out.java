package p;

class A {
	/**
	 * @see #use(Object)
	 */
	public void use(Object arg) {
		System.out.println(arg);
	}
	
	public void call() {
		use(null);
		use(null);
		use(null);
		use(null);
		use(null);
		use(null);
		use(null);
	}
}