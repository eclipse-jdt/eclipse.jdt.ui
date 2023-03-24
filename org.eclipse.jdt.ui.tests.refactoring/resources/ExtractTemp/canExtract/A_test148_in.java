package p; // 7, 9, 7, 19

public class A {
	static int a= 1;

	void foo() {
		getValue();
		A.a++;
		int a= getValue();
		A.a++;
		int b= getValue();
	}

	int getValue() {
		return (A.a);
	}
}
