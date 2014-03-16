package p;

public interface A {

	public abstract void m();
}
class AImpl {

	A i1 = () -> System.out.println();
	A i2 = () -> {
		System.out.println();
	};
	A i3 = new A() {

		@Override
		public void m() {
			System.out.println();
		}
	};
	
	private void foo() {
		A i4 = () -> {
			System.out.println();
		};

	}
}