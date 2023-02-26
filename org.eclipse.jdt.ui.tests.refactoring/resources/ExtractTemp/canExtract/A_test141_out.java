package p; // 5, 20, 5, 32

public class A {
	void foo(B b) {
		int value= b.getValue();
		int init = value;
		while (b.getValue() - init > 5)
			b.decI();
		int value2= b.getValue();
		System.out.println(init * value2);
	}
}

class B {
	int i;
	B b1;

	public B(int i) {
		super();
		this.i = i;
		b1 = new B(i % 10);
	}

	int decI() {
		return --i;
	}

	int getValue() {
		return i * 10 + b1.i;
	}

}
