//abstract and make private, only getter
package p;
class A{
	public int f;
	void m(){
		f= f;
	}
}
class B{
	int m(){
		A a= new A();
		a.f= a.f;
		return a.f;
	}
}