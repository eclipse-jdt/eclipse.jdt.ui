package p;

public class VariableDeclarationInitializer {
	private int test = 5;

	public void foo() {
		int x = test;
		int test = this.test;
	}
}