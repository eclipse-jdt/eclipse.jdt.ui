package invalid;

public class TestLocalInitializer {
	
	public void foo() {
		int i= /*]*/bar()/*[*/;
	}

	public int bar() {
		if (true)
			return 1;
		else
			return 2;
	}
}
