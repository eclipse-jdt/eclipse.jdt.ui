package invalid;

public class TestInvalidFieldInitializer2 {
	
	private int field= /*]*/foo(null)/*[*/;	
	
	public Object foo(Object obj) {
		return obj = new Object();
	}
}
