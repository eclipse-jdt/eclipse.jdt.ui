package p;

public class TestSimpleEnclosingCodeStyle {
	public static class FooParameter {
		public String[] fStringsG;
		public int fBG;
		public FooParameter(String[] aStringsM, int aBM) {
			fStringsG = aStringsM;
			fBG = aBM;
		}
	}

	public void foo(FooParameter parameterObject){
		int b = parameterObject.fBG;
		System.out.println(parameterObject.fStringsG[0]);
		b++;
	}
	
	public void fooCaller(){
		foo(new FooParameter(new String[]{"Test"}, 6));
	}
}
