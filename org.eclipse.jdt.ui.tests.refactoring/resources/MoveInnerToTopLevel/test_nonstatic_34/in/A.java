package p;
class A {

	public class Inner {

		public Inner() {
			super();
			System.out.println(getName());
		}

		public String getName() {
			return getTopName() + ".Inner";
		}
	}

	public A() {
		new Object(){};
		System.out.println(new Inner().getName());
	}

	public String getTopName() {
		return "Top";
	}

	static public void main(String[] argv) {
		new A();
	}
}
