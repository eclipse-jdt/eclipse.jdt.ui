//making f abstract not possible - calls to A's constructors
package p;

class A {
	public void f(){
	}
}
class B extends A {
	void g(){
		A a= new A();
	}
}