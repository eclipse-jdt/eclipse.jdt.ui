
public class TestDefaultPackagePointTopLevel {
	public static void main(String[] args) {
		new TestDefaultPackagePointTopLevel().foo(new ArrayList(5, 6));
	}
	public void foo(ArrayList parameterObject){
		System.out.println(parameterObject.xer+parameterObject.yer);
	}
}
