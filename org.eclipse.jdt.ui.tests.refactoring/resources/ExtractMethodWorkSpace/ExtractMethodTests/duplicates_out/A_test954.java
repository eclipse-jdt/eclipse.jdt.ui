package duplicates_out;

public class A_test954 {
	void foo() {
		extracted();
		if (true)
			extracted();
	}

	protected void extracted() {
		/*[*/System.out.println("Eclipse");/*]*/
	}
}
