package p;

public class InheritanceUpdateGetterSetter {
	protected String test;
	protected int test2;

	public void foo() {
		this.test=test;
		new InheritanceUpdateGetterSetter().test=this.test;
	}
}