package invalid;

public class TestInvalidFieldInitializer3 {
	
	private Object field= /*]*/foo()/*[*/;	
	
	public Object foo() {
		return field;
	}
}
