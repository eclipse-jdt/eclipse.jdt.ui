package p;

public interface A {

	public abstract void m(int i, int j);
}
class AImpl {

	A i1 = (int i, int j) -> System.out.println();
	A i2 = (i, j) -> {
		System.out.println();
	};
	A i3 = new A() {

		@Override
		public void m(int i, int j) {
			System.out.println();
		}
	};

}