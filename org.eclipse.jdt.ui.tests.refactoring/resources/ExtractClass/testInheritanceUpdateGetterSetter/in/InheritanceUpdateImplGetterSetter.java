package p;

public class InheritanceUpdateImplGetterSetter extends InheritanceUpdateGetterSetter{
	protected String test="Test";
	public void foo() {
		System.out.println("Test:"+test+" Super:"+super.test);
		test2++;
		super.test2*=2;
	}
}