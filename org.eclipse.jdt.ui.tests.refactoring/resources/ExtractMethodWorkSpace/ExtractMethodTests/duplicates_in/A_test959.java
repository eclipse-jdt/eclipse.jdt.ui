package duplicates_in;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test959 {
	public void foo() {
		int x= 10;
		int y= /*[*/x/*]*/;
		x= 20;
	}
}
