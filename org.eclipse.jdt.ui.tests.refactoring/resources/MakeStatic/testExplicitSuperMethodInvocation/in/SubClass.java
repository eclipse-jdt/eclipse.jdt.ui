public class SubClass extends SuperClass {

	public void bar() {
		super.instanceParent();
	}

	@Override
	public void instanceParent() {
		System.out.println("def");
	}
}
