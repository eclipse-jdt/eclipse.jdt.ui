public class SubClass extends SuperClass {

	public void bar() {
		instanceParent();
	}

	@Override
	public void instanceParent() {
		System.out.println("def");
	}
}
