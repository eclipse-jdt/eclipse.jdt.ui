package cast_in;

public class TestInfixExpression2 {	
	long x(long two) {
		return two * Integer.MAX_VALUE;
	}

	void foo() {
		long much = /*]*/x(2 * Integer.MAX_VALUE)/*[*/;
	}
}
