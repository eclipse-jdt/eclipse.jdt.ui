package duplicates_in;

public class A_test953 {
	void foo() {
		int i =10;
		/*[*/bar(i);/*]*/
		
		int j= 10;
		bar(j);
	}

	void bar(int x) {
	}
}
