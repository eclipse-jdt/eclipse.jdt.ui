package p;

class A {
	/**
	 * @see #use(Object, String[])
	 * @deprecated Use {@link #use(Object)} instead
	 */
	public void use(Object first, String... args) {
		use(first);
	}

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