package argument_in;

public class TestParameterNameUsed3 {
	public void main() {
		/*]*/foo(10);/*[*/
		{
			int x= 20;
		}
	}
	
	public void foo(int x) {
		x= 20;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
