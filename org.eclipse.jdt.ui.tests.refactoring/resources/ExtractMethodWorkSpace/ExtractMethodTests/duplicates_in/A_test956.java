package duplicates_in;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test956 {
	void foo() {
		int y= /*[*/2/*]*/;
	}
	class Inner {
		void foo() {
			int y1= 2;
		}
	}	
}
