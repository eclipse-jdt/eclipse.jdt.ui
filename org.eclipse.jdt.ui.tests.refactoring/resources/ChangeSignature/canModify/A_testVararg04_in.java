package p;

class A {
	public void use(String... args) { }
	
	public void call() {
		use();
		use("one");
		use("one", "two");
		use(new String[] {"one", "two"});
		use(null);
		use((String[]) null);
		use((String) null);
	}
}