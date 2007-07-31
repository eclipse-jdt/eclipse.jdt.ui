package p;

public class InheritanceUpdateGetterSetter {
	protected InheritanceUpdateGetterSetterParameter parameterObject = new InheritanceUpdateGetterSetterParameter();

	public void foo() {
		this.parameterObject.setTest(parameterObject.getTest());
		new InheritanceUpdateGetterSetter().parameterObject.setTest(this.parameterObject.getTest());
	}
}