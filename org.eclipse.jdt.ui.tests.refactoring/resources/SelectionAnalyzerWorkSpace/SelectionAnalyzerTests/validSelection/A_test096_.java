package validSelection;

public class A_test096_ {
	public void foo() {
		int i= 10;
		switch(i) {
			case 10:
				foo();
				/*]*/break/*[*/;
		}
	}	
}