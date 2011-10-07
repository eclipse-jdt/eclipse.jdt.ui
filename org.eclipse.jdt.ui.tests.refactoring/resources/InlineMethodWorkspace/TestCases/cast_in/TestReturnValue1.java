package cast_in;

public class TestReturnValue1 {	
	long foo() {
		return 1;
	}

	void x() {
		long much = /*]*/foo()/*[*/;
	}
}
