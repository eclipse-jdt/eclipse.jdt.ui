package cast_in;

public class TestReturnValue2 {	
	long foo() {
		return 1 + 1;
	}

	void x() {
		long much = /*]*/foo()/*[*/ * Integer.MAX_VALUE;
	}
}
