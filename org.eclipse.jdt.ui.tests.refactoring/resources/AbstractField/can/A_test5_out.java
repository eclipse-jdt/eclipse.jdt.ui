//abstract and make private
package p;
class A{
	private int f;
	void m(){
		setF(getF());
	}
	public int getF(){
		return f;
	}
	public void setF(int f){
		this.f= f;
	}
}
class B{
	int m(){
		A a= new A();
		a.setF(a.getF());
		return a.getF();
	}
}