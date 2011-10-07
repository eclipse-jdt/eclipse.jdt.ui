package cast_out;

public class TestReturnValue2 {	
	long foo() {
		return 1 + 1;
	}

	void x() {
		long much = (long) (1 + 1) * Integer.MAX_VALUE;
	}
}
