package argument_out;

public class TestParameterNameUsed1 {
	public void main() {
		int x= 20;
		int x1 = 10;
		x1= 20;
		bar(x1);
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
