package invalid;

public class TestMultiLocal {
	
	public void foo() {
		int i= /*]*/bar()/*[*/, x;
	}

	public int bar() {
		if (true)
			return 1;
		else
			return 2;
	}
}
