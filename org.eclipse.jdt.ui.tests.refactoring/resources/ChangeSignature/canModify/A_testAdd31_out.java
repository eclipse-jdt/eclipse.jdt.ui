package p;
class A{
	void m(int i, int x){
		m(i, 0);
	}
}
class B extends A{
	void m(int j, int x){
		super.m(j, 0);
		this.m(j, 0);
		m(j, 0);
	}
}