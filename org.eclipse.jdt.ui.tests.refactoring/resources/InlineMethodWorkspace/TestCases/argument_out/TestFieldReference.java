package argument_out;

public class TestFieldReference {
	private String field;
	
	public void main() {
		field.toString();
	}

	public void bar(Object o) {
		o.toString();
	}
}
