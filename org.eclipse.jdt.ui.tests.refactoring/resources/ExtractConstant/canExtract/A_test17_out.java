//5, 16 -> 5, 35   AllowLoadtime == false
package p;
class A {
    private static final int YET_ANOTHER_FRED= getYetAnotherFred();
	void f() {
        int i= YET_ANOTHER_FRED;
    }
    static int getYetAnotherFred(){
        return 5;
    }
}