package invalidSelection;

public class A_test096 {
	public void foo() {
		int i= 10;
		switch(i) {
			case 10:
				foo();
				/*]*/break/*[*/;
		}
	}	
}