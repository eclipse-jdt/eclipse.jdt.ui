package nameconflict_in;

public class TestSwitchStatement {
	public void main() {
		int i= 10;
		switch(i) {
			case 0:
				break;
			case 10:
				/*]*/foo();/*[*/
				break;
		}
	}
	
	public void foo() {
		int i= 20;
		i++;
	}
}
