package p;

public class NestedClass_in {
	public class InnerClass {
		public InnerClass() { }
	}

	public void foo() {
		InnerClass	ic= /*[*/new InnerClass()/*]*/;
	}
}
