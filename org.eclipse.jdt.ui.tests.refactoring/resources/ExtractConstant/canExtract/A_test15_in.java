//5, 16 -> 5, 25   AllowLoadtime == false
package p;
class A {
	void f() {
		int i= getFred();
	}
	static int getFred(){
		return 5;
	}
}