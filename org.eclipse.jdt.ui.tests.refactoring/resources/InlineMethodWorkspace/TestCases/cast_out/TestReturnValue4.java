package cast_out;

public class TestReturnValue4 {	
	Integer foo() {
		return 1 + 1;
	}

	void x() {
		int a= ((Integer) (1 + 1)).intValue();
	}
}
