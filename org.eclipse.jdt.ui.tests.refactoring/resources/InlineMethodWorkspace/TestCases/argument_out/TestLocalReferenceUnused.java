package argument_out;

public class TestLocalReferenceUnused {
	public void main() {
		int i= 10;
		i= i + 10;
		bar(i);
		int x= 7;
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
