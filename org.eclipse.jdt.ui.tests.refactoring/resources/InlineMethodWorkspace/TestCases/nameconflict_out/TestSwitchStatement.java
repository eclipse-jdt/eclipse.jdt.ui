package nameconflict_out;

public class TestSwitchStatement {
	public void main() {
		int i= 10;
		switch(i) {
			case 0:
				break;
			case 10:
				int i1= 20;
				i1++;
				break;
		}
	}
	
	public void foo() {
		int i= 20;
		i++;
	}
}
