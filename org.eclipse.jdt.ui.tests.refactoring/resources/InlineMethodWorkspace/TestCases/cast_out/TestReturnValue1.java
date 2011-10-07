package cast_out;

public class TestReturnValue1 {	
	long foo() {
		return 1;
	}

	void x() {
		long much = /*]*/(long) 1/*[*/;
	}
}
