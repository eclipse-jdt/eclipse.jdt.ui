package p;
class A{
	protected int g;
	void m(){
		g++;
	}
}
class B{
	A a;
	void m(){
		a.g= 0;
	}
}