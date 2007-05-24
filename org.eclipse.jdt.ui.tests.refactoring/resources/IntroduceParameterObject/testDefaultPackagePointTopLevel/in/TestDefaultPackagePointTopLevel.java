
public class TestDefaultPackagePointTopLevel {
	public static void main(String[] args) {
		new TestDefaultPackagePointTopLevel().foo(5, 6);
	}
	public void foo(int xer, int yer){
		System.out.println(xer+yer);
	}
}
