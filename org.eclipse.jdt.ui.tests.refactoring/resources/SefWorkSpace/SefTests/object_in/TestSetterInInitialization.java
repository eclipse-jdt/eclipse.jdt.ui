package object_in;

public class TestSetterInInitialization {
	String field;
	
	public void foo() {
		TestSetterInInitialization a= null;
		String t= a.field= "d";
	}
}
