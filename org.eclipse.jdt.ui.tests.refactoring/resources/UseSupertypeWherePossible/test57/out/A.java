package p;

class A implements I {
	public void m() {}
	public void m1() {}
	protected I f(){
		return this;
	}
	void test(){
		f().m();
	}
}