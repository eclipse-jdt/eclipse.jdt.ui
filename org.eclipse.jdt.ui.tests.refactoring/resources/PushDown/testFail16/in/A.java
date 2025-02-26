package p;

class A {
	void m() {
	}
}
class B extends A {
	public static void main(String[] args) {
		new A().m();
	}
}