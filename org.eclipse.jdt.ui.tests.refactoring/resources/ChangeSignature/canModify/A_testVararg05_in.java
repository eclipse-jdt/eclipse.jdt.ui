package p;

class A {
	/**
	 * @see #use(Object, String[])
	 */
	public void use(Object first, String... args) {
		System.out.println(first);
	}
	
	public void call() {
		use(null);
		use(null, "one");
		use(null, "one", "two");
		use(null, new String[] {"one", "two"});
		use(null, null);
		use(null, (String[]) null);
		use(null, (String) null);
	}
}