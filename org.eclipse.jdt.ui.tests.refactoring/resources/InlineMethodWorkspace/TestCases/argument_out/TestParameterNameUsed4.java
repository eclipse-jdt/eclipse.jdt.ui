package argument_out;

public class TestParameterNameUsed4 {
	public void main() {
		class x {}
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
