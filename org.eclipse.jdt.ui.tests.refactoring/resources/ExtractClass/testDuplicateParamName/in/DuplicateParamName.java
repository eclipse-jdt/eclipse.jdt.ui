package p;

public class DuplicateParamName {
	private String fHTest= foo();
	private String fGTest= foo();
	private String foo() {
		return "Foo";
	}
}
