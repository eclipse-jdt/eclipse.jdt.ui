package p;

public class VariableDeclarationInitializer {
	private VariableDeclarationInitializerParameter parameterObject = new VariableDeclarationInitializerParameter(5);

	public void foo() {
		int x = parameterObject.getTest();
		int test = this.parameterObject.getTest();
	}
}