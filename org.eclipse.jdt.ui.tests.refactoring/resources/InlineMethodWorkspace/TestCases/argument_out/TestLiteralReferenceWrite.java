package argument_out;

public class TestLiteralReferenceWrite {
	public void main() {
		int x = 10;
		x= 20;
		bar(x);
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
