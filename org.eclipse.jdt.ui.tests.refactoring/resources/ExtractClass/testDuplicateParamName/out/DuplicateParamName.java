package p;

public class DuplicateParamName {
	private DuplicateParamNameParameter parameterObject = new DuplicateParamNameParameter(foo(), foo());
	private String foo() {
		return "Foo";
	}
}
