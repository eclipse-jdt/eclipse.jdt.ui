package invalid;

public class TestFieldInitializer {
	
	private int field= /*]*/foo()/*[*/;	
	
	public int foo() {
		return 1;
	}
}
