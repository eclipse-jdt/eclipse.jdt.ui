package object_in;

public class TestSetterInAssignment {
	String field;
	
	public void foo() {
		TestSetterInAssignment a= null;
		String t= null;
		t= a.field= "d";
	}
}
