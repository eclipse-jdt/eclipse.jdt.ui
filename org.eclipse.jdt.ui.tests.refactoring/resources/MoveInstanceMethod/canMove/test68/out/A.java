public class A extends C {
	public A() {
		i= 1;
	}

	public static void main(String[] args) {
		System.out.println(new D().m(new A()));
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

	public int m(A a) {
		B b= new B() {
			public int n() {
				return i;
			}
		};
	
		return b.n();
	}

}
