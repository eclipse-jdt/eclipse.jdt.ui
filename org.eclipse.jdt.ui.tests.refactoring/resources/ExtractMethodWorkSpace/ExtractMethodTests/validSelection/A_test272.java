package validSelection;

public class A_test272 {
	public void foo() {
		switch(1) {
			case 1:
				foo();
				/*]*/foo()/*[*/;
				foo();
		}
	}
}