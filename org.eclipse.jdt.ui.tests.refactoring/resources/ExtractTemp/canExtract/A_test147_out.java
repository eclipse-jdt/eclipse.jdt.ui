package p; // 10, 22, 10, 32

public class A {
	static int a = 1;

	void foo() {
		int value= getValue();
		{
			int a = value;
			a += 2;
			int a2 = value;
		}
		{
			int a3 = value;
			a += 2;
			int value2= getValue();
			int a4 = value2;
		}
		int value3= getValue();
		int a5 = value3;
	}

	int getValue() {
		return (A.a);
	}
}
