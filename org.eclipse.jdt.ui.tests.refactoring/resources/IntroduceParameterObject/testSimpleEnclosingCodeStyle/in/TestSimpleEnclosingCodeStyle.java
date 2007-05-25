package p;

public class TestSimpleEnclosingCodeStyle {
	public void foo(final String[] strings, int b){
		System.out.println(strings[0]);
		b++;
	}
	
	public void fooCaller(){
		foo(new String[]{"Test"},6);
	}
}
