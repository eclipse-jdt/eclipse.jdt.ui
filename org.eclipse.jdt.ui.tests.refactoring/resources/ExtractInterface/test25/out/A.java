package p;

class A implements I {
	public void m() {}
	protected I g() {
		return this;	
	}
}