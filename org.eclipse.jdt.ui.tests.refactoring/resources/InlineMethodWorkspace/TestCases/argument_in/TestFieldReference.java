package argument_in;

public class TestFieldReference {
	private String field;
	
	public void main() {
		/*]*/bar(field);/*[*/
	}

	public void bar(Object o) {
		o.toString();
	}
}
