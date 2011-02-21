package cast_out;

public class TestInfixExpression1 {	
	long x(long two) {
		return two * Integer.MAX_VALUE;
	}

	void foo() {
		long much = (long) (1 + 1) * Integer.MAX_VALUE;
	}
}
