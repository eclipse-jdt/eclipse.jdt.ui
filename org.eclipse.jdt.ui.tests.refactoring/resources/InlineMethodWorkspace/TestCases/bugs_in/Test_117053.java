package bugs_in;

public class Test_117053 {
	void foo(int x) {
		if (x++ < 6)
			x++;
	}
	void bar() {
		int x = 2;
		if (x < 10)
			x++;
		else
			/*]*/foo(x);/*[*/
	}
}
