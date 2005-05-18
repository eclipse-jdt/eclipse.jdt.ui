package bugs_in;

public class Test_76241 {
	protected void toInline(String s) {
		System.out.println("A.foo()");
	}

	protected void toInline(Integer i) {
		System.out.println("A.foo()");
	}
}

class Test_76241_B1 extends Test_76241 {
	public void bar1() {
		toInline(null);
	}
}

class Test_76241_B2 extends Test_76241 {
	public void bar2() {
		toInline(null);
	}
}