package p;

class A {
	public void m(int i, String[] names) {
	}
}

class B extends A {
	public void m(int i, String... names) {
	}	
}

class Client {
	void test(int i, String... args) {
		new A().m(1, new String[] {"X"});
		new B().m(0);
		new B().m(2, "X", "Y", "Z");
	}
}
