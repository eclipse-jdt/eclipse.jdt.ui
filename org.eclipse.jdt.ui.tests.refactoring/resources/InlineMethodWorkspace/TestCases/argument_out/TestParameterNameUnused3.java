package argument_out;

public class TestParameterNameUnused3 {
	public void main() {
		class T {
			int x;
		}
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
