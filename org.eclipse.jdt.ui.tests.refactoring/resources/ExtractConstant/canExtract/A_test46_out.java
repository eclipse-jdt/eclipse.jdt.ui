package p; //8, 17, 8, 22

public class A {
	private static final int x = 9;
	private static final int y = 10;
	private static final int CONSTANT= x + y;
	private static final int k = 8;
    void m() {
        int j = CONSTANT + 30 + k + CONSTANT + k + k + 40 + CONSTANT + k + y + x;
    }
}
