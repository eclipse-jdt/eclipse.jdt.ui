//pushing f not possible - referenced by getF
package p;

class A {
	
	private int f;

	public int getF() {
		return f;
	}
}
class B extends A {
}