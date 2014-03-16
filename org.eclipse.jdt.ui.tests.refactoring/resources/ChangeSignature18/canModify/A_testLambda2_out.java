package p;

public interface A {

	public abstract void m(int x);
}
class AImpl {

	A i1 = (x) -> System.out.println();
	A i2 = (x) -> {
		System.out.println();
	};
	A i3 = new A() {

		@Override
		public void m(int x) {
			System.out.println();
		}
	};
	
	private void foo() {
		A i4 = (x) -> {
			System.out.println();
		};

	}
}