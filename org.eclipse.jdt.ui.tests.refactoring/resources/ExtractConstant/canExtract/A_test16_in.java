//5, 20 -> 5, 27   AllowLoadtime == false
package p;
class A {
	void f() {
		boolean i= isRed();
	}
	static boolean isRed(){
		return 5==1;
	}
}