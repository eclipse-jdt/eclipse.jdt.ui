package p;

public interface A {

	public abstract void m(int i);
}
class AImpl {

	A i1 = (int i) -> System.out.println();
	A i2 = i -> {
		System.out.println();
	};
	A i3 = new A() {

		@Override
		public void m(int i) {
			System.out.println();
		}
	};
	
	private void foo() {
		A i4 = i -> {
			System.out.println();
		};

	}
}