//5, 16 -> 5, 35   AllowLoadtime == false
package p;
class A {
	private static final int CONSTANT= getYetAnotherFred();
	void f() {
		int i= CONSTANT;
	}
	static int getYetAnotherFred(){
		return 5;
	}
}