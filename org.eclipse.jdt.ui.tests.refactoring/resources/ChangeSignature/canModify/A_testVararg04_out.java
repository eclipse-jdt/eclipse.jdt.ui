package p;

class A {
	public void use(int i, String... args) { }
	
	public void call() {
		use(1);
		use(1, "one");
		use(1, "one", "two");
		use(1, new String[] {"one", "two"});
		use(1, null);
		use(1, (String[]) null);
		use(1, (String) null);
	}
}