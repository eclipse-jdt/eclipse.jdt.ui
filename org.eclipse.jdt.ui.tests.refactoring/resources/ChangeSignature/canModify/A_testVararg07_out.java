package p;

class A {
	public void m(int i, String j, Integer k) {
	}
}

class B extends A {
	public void m(int i, String j, Integer k) {
	}	
}

class Client {
	void test(int i, String... args) {
		new A().m(1, "none", 17);
		new B().m(0, "none", 17);
		new B().m(2, "none", 17);
	}
}
