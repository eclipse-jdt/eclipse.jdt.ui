package argument_out;

public class TestParameterNameUnused1 {
	public void main() {
		{
			int x= 20;
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
