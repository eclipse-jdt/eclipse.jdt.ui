//5, 16 -> 5, 25   AllowLoadtime == false
package p;
class A {
	private static final int CONSTANT= getFred();
	void f() {
		int i= CONSTANT;
	}
	static int getFred(){
		return 5;
	}
}