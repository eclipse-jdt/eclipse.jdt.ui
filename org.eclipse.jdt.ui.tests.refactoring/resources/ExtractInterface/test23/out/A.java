package p;

class A implements I {
	public void m() {}
	void g() {
		I a= (I)new A();
	}
}