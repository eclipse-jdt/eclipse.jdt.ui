package p;

public interface A {

	public abstract void m(int j, int i);
}
class AImpl {

	A i1 = (int j, int i) -> System.out.println();
	A i2 = (j, i) -> {
		System.out.println();
	};
	A i3 = new A() {

		@Override
		public void m(int j, int i) {
			System.out.println();
		}
	};

}