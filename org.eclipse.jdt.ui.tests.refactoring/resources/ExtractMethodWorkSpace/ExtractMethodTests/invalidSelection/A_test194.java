package invalidSelection;

public class A_test194 {
	A_test194 fff() {
		return this;
	}
	int yyy() {
		return 32;
	}
	void g() {
		int f = /*[*/fff/*]*/().yyy();
	}
}