package argument_out;

public class TestLocalReferenceRead {
	public void main() {
		int foo = 0;
		int bar = foo;
		bar++;
		System.out.println(foo);
	}
	
	public void inlineMe(int bar) {
		bar++;
	}
}
