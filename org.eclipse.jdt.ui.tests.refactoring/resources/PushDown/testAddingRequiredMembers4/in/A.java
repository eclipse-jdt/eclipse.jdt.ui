//nothing added to m, f
package p;
class A{	
	protected void m(){
		f();
	}
	private void f(){}
}
class B extends A{
}