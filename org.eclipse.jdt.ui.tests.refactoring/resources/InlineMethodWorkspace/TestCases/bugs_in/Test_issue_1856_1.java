package bugs_in;

public class Test_issue_1856_1 {
	String foo() {
		return "abc"; //$NON-NLS-1$
	}
	String bar() {
		String result = /*]*/foo()/*[*/;
		return result;
	}
}
