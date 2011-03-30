package cast_out;

public class TestReturnValue3 {	
	Integer foo() {
		return 1;
	}

	void x() {
		int a= ((Integer) 1).intValue();
	}
}
