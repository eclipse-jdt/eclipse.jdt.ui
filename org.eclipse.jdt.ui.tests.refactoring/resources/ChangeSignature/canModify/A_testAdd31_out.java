package p;
class A{
	void m(int i, int x){
		m(i, x);
	}
}
class B extends A{
	void m(int j, int x){
		super.m(j, x);
		this.m(j, x);
		m(j, x);
	}
}