package argument_in;

public class TestLocalReferenceRead {
	public void main() {
		int i= 10;
		/*]*/foo(i);/*[*/
		System.out.println(i);
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
