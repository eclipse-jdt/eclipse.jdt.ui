package argument_out;

public class TestLocalReferenceRead {
	public void main() {
		int i= 10;
		int x = i;
		/*]*/x= x + 10;
		bar(x);/*[*/
		System.out.println(i);
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
