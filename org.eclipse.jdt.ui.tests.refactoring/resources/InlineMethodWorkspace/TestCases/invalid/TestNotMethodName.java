package invalid;

public class TestNotMethodName {
	private static Class thisClass= TestNotMethodName.class;
	static {
        System.out.println(/*]*/thisClass/*[*/);
	}
}
