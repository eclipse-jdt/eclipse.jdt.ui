package validSelection;

public class A_test275 {
	public void foo() {
		/*]*/foo();/*[*/
		
		switch(1) {
			case 1:
				foo();
			case 2:
				foo();
		}		
	}
}