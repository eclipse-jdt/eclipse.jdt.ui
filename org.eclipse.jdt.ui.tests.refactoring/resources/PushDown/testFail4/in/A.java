//no access to m() from f
package p;

class A {
	public void f(){
		m();
	}
	private void m(){
	}
}
class B extends A {
}