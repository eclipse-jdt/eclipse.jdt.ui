package p;

class A {
	public void m(int i, String... names) {
		for (String name : names) {
			System.out.println(name);
		}
	}
}