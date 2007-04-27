package p;

public class TestReorderGetter {
	public static class FooParameter {
		/**
		 * 
		 */
		public double d;
		/**
		 * 
		 */
		public String a;
		/**
		 * 
		 */
		public int b;
		/**
		 * 
		 */
		public FooParameter(double d, String a, int b) {
			this.d = d;
			this.a = a;
			this.b = b;
		}
		/**
		 * @return the d
		 */
		public double getD() {
			return d;
		}
		/**
		 * @return the a
		 */
		public String getA() {
			return a;
		}
		/**
		 * @return the b
		 */
		public int getB() {
			return b;
		}
	}

	public void foo(FooParameter parameterObject){
		double d = parameterObject.getD();
		System.out.println(parameterObject.getA()+parameterObject.getB());
		d++;
	}
}
