package base_in;

public class TestThisExpression {
	int field;
	
	public void foo() {
		this.field= 10;
		new TestThisExpression().field= 11;
	}
}