package p;

public class TestSimpleEnclosing {
	public void foo(final String[] a, int b){
		System.out.println(a[0]);
		b++;
	}
	
	public void fooCaller(){
		foo(new String[]{"Test"},6);
	}
}
