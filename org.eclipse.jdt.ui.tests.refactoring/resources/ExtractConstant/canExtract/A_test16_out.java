//5, 20 -> 5, 27   AllowLoadtime == false
package p;
class A {
    private static final boolean RED= isRed();
	void f() {
        boolean i= RED;
    }
    static boolean isRed(){
        return 5==1;
    }
}