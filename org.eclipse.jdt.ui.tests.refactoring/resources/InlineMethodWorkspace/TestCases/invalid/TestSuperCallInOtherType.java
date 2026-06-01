package invalid;

class B extends A {
	int toInline() {
		return super.g();
	}
}

public class TestSuperCallInOtherType {
	public static void main(String[] args) {
		B b = new B();
		new C().test(b);
	}
}

class A {
	int g() {
		return 1;
	}
}


class C {
	void test(B b) {
		int x = /*[*/b.toInline()/*]*/; // inline method 'f()'
		System.out.println(x);
	}
}
