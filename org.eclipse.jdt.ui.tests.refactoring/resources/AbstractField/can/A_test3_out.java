//abstract and make private
package p;
class A{
	private int f;
	void m(){
		int g= getF();
	}
	public int getF(){
		return f;
	}
}
class B{
	int m(){
		A a= new A();
		return a.getF();
	}
}