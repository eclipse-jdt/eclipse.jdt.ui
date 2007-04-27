package p;

class TestDelegateCreationA {
	int b;
}

public class TestDelegateCreation extends TestDelegateCreationA{
	String a[];
	public static class FooParameter {
		/**
		 * 
		 */
		public String[] newA;
		/**
		 * 
		 */
		public int newB;
		/**
		 * 
		 */
		public double newD;
		/**
		 * 
		 */
		public FooParameter(String[] newA, int newB, double newD) {
			this.newA = newA;
			this.newB = newB;
			this.newD = newD;
		}
		/**
		 * @return the newA
		 */
		public String[] getNewA() {
			return newA;
		}
		/**
		 * @param newA the newA to set
		 */
		public void setNewA(String[] newA) {
			newA = newA;
		}
		/**
		 * @return the newB
		 */
		public int getNewB() {
			return newB;
		}
		/**
		 * @param newB the newB to set
		 */
		public void setNewB(int newB) {
			newB = newB;
		}
		/**
		 * @return the newD
		 */
		public double getNewD() {
			return newD;
		}
		/**
		 * @param newD the newD to set
		 */
		public void setNewD(double newD) {
			newD = newD;
		}
	}
	/**
	 * @deprecated Use {@link #foo(FooParameter)} instead
	 */
	public void foo(String[] a, int b, double d){
		foo(new TestDelegateCreation.FooParameter(a, b, d));
	}
	public void foo(FooParameter parameterObject){
		double d = parameterObject.getNewD();
		int b = parameterObject.getNewB();
		String[] a = parameterObject.getNewA();
		a=new String[0];
		d=5.7;
		b=6;
	}
}
