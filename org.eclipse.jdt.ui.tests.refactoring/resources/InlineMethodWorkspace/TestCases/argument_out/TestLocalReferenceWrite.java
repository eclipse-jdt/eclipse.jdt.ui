package argument_out;

public class TestLocalReferenceWrite {
	public void main() {
		int i= 10;
		i= i + 10;
		bar(i);
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
