package argument_in;

public class TestLocalReferenceWrite {
	public void main() {
		int i= 10;
		/*]*/foo(i);/*[*/
		i= 10;
		System.out.println(i);
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
