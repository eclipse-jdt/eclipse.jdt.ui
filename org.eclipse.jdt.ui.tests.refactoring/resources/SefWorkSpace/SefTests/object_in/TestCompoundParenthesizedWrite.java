package object_in;

public class TestCompoundParenthesizedWrite {
	public String field;
	
	public void foo(TestCompoundParenthesizedWrite other) {
		(other.field)= "field";
	}
}

