package p; // 10, 22, 10, 32

public class A {
	static int a = 1;

	void foo() {
		{
			int a = getValue();
			a += 2;
			int a2 = getValue();
		}
		{
			int a3 = getValue();
			a += 2;
			int a4 = getValue();
		}
		int a5 = getValue();
	}

	int getValue() {
		return (A.a);
	}
}
