//abstract and make private (do not abstract private accesses)
package p;
class A{
	public int f;
	void m(){
		int g= f;
	}
}
class B{
	int m(){
		A a= new A();
		return a.f;
	}
}