package p;

class A implements I {
	I fA;
	public void m() {}
	public void m1() {}
	void f(){
		I a= fA;
		a.m();
	}
}