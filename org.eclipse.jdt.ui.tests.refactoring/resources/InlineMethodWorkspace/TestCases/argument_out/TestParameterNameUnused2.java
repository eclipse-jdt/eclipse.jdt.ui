package argument_out;

public class TestParameterNameUnused2 {
	public void main() {
		for (int x= 10; x < 20; x++)
			main();
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
