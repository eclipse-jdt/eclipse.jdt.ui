package argument_in;

public class TestLocalReferenceRead {
	public void main() {
		int foo = 0;
		/*]*/inlineMe(foo);/*[*/
		System.out.println(foo);
	}
	
	public void inlineMe(int bar) {
		bar++;
	}
}
