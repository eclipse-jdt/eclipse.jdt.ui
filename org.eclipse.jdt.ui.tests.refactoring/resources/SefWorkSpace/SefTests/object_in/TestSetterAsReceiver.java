package object_in;

public class TestSetterAsReceiver {
	String field;
	
	public void foo() {
		TestSetterAsReceiver a= null;
		(a.field= "d").length();
	}
}
