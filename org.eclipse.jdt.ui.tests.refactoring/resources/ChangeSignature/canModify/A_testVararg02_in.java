package p;

class A {
	public void m(int i, String... names) {
		for (String name : names) {
			System.out.println(name);
		}
	}
}

class B extends A {
	public void m(int i, String[] names) {
		for (String name : names) {
			System.out.println(name);
		}
	}
}

class Client {
	{
		new A().m(0);
		new A().m(2, "X", "Y", "Z");
		new B().m(1, new String[] {"X"});
	}
}