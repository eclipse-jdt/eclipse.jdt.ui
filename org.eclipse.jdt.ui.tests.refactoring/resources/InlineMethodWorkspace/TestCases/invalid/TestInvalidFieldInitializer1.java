package invalid;

public class TestInvalidFieldInitializer1 {
	
	private int field= /*]*/foo()/*[*/;	
	
	public int foo() {
		int a= 0;
		return 1;
	}
}
