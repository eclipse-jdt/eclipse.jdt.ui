//no access to i from f
package p;

class A {
	public void f(){
		i++;
	}
	private int i;
}
class B extends A {
}