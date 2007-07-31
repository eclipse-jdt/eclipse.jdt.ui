package p;

public class InheritanceUpdateImplGetterSetter extends InheritanceUpdateGetterSetter{
	protected String test="Test";
	public void foo() {
		System.out.println("Test:"+test+" Super:"+super.parameterObject.getTest());
		parameterObject.setTest2(parameterObject.getTest2() + 1);
		super.parameterObject.setTest2(super.parameterObject.getTest2() * 2);
	}
}