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

class C extends B {
	public void m(int i, String... names) {
		System.out.println(names[i]);
		names= new String[0];
	}
}

class Client {
	{
		new A().m(0);
		new B().m(1, new String[] {"X"});
		new C().m(2, new String[] {"X", "Y"});
		new C().m(2, "X", "Y", "Z");
	}
}