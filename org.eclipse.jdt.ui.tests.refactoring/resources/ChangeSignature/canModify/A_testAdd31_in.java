package p;
class A{
	void m(int i){
		m(i);
	}
}
class B extends A{
	void m(int j){
		super.m(j);
		this.m(j);
		m(j);
	}
}