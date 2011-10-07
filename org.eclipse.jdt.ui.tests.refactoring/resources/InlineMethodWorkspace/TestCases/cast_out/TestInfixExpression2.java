package cast_out;

public class TestInfixExpression2 {	
	long x(long two) {
		return two * Integer.MAX_VALUE;
	}

	void foo() {
		long much = (long) (2 * Integer.MAX_VALUE) * Integer.MAX_VALUE;
	}
}
