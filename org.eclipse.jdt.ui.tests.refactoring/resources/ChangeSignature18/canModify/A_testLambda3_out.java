package p;

public interface A {

	public abstract void newName(String s);
}
class AImpl {

	A i1 = (String s) -> System.out.println();
	A i2 = (s) -> {
		System.out.println();
	};
	A i3 = new A() {

		@Override
		public void newName(String s) {
			System.out.println();
		}
	};
	
	private void foo() {
		A i4 = s -> {
			System.out.println();
		};

	}
}