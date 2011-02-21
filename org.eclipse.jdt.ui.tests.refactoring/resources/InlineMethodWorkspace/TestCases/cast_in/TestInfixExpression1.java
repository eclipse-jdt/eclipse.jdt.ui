package cast_in;

public class TestInfixExpression1 {	
	long x(long two) {
		return two * Integer.MAX_VALUE;
	}

	void foo() {
		long much = /*]*/x(1 + 1)/*[*/;
	}
}
