//5, 16 -> 5, 35   AllowLoadtime == false
package p;
class A {
	void f() {
		int i= getYetAnotherFred();
	}
	static int getYetAnotherFred(){
		return 5;
	}
}