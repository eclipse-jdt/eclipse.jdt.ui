//5, 16 -> 5, 25   AllowLoadtime == false
package p;
class A {
    private static final int FRED= getFred();
	void f() {
        int i= FRED;
    }
    static int getFred(){
        return 5;
    }
}