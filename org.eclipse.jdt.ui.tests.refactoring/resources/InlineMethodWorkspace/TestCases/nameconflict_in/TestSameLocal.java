package nameconflict_in;

public class TestSameLocal {
	public void main() {
		int i= 10;
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		int i= 20;
		i++;
	}
}
