public class SubClass extends SuperClass {

	public static void bar(SubClass subClass) {
		subClass.instanceParent();
	}

	@Override
	public void instanceParent() {
		System.out.println("def");
	}
}
