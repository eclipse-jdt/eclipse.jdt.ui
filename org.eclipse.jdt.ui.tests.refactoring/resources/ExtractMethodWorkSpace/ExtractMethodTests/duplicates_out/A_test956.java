package duplicates_out;

// don't extract second occurence of
// 2 since it is in a inner class
public class A_test956 {
	void foo() {
		int y= extracted();
	}
	protected int extracted() {
		return /*[*/2/*]*/;
	}
	class Inner {
		void foo() {
			int y1= 2;
		}
	}	
}
