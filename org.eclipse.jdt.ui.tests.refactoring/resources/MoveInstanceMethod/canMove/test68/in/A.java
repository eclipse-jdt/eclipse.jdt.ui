public class A extends C {
	public A() {
		i= 1;
	}

	public int m(D d) {
		B b= new B() {
			public int n() {
				return i;
			}
		};

		return b.n();
	}

	public static void main(String[] args) {
		System.out.println(new A().m(new D()));
	}
}

class B extends C {
	public int n() {
		return -1;
	}
}

class C {
	public int i= 0;
}

class D {

}
