package argument_out;

public class TestLocalReferenceLoop {
	public void main() {
		int i= 10;
		for (int z= 0; z < i; z++) {
			int x = i;
			x= x + 10;
			bar(x);
		}
	}
	
	public void foo(int x) {
		x= x + 10;
		bar(x);
	}
	
	public void bar(int z) {
	}
}
