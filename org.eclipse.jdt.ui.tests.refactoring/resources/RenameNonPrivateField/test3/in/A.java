package p;
class A{
	protected int f;
	void m(){
		f++;
	}
}
class B{
	A a;
	void m(){
		a.f= 0;
	}
}