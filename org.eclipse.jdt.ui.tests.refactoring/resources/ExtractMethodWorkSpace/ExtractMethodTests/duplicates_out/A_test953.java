package duplicates_out;

public class A_test953 {
	void foo() {
		int i =10;
		extracted(i);
		
		int j= 10;
		extracted(j);
	}

	protected void extracted(int i) {
		/*[*/bar(i);/*]*/
	}

	void bar(int x) {
	}
}
