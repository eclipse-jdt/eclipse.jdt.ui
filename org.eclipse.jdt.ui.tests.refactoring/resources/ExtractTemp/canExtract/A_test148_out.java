package p; // 7, 9, 7, 19

public class A {
	static int a= 1;

	void foo() {
		int value= getValue();
		A.a++;
		int value2= getValue();
		int a= value2;
		A.a++;
		int value3= getValue();
		int b= value3;
	}

	int getValue() {
		return (A.a);
	}
}
