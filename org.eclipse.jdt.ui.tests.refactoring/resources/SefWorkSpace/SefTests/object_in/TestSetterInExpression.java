package object_in;

public class TestSetterInExpression {
	String field;
	
	public void foo() {
		TestSetterInExpression a= null;
		if ((a.field= "d") == "d")
			foo();
	}
}
