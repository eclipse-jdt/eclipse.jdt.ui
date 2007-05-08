package p;

class TestDelegateCreationA {
	int b;
}

public class TestDelegateCreationCodeStyle extends TestDelegateCreationA{
	String a[];
	public static class FooParameter {
		private String[] fAG;
		private int fBG;
		private double newD;
		public FooParameter(String[] fAG, int fBG, double newD) {
			this.fAG = fAG;
			this.fBG = fBG;
			this.newD = newD;
		}
		public String[] getA() {
			return fAG;
		}
		public void setA(String[] a) {
			fAG = a;
		}
		public int getB() {
			return fBG;
		}
		public void setB(int b) {
			fBG = b;
		}
		public double getNewD() {
			return newD;
		}
		public void setNewD(double newD) {
			this.newD = newD;
		}
	}
	/**
	 * @deprecated Use {@link #foo(FooParameter)} instead
	 */
	public void foo(String[] a, int b, double d){
		foo(new FooParameter(a, b, d));
	}
	public void foo(FooParameter parameterObject){
		double d = parameterObject.getNewD();
		int b = parameterObject.getB();
		String[] a = parameterObject.getA();
		a=new String[0];
		d=5.7;
		b=6;
	}
}
