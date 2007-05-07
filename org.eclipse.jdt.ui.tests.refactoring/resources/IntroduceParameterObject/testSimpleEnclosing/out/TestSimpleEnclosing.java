package p;

public class TestSimpleEnclosing {
	public static class FooParameter {
		public String[] a;
		public int b;
		public FooParameter(String[] a, int b) {
			this.a = a;
			this.b = b;
		}
	}

	public void foo(FooParameter parameterObject){
		int b = parameterObject.b;
		System.out.println(parameterObject.a[0]);
		b++;
	}
	
	public void fooCaller(){
		foo(new FooParameter(new String[]{"Test"}, 6));
	}
}
