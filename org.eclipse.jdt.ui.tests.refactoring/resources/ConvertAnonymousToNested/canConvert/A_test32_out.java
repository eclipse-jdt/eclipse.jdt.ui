package p;
public class A {

	private final class Inner1Extension extends Inner1 {
		private final String param;
		private Inner1Extension(int[] param, String param2) {
			super(param);
			this.param= param2;
		}
		public void m1(String s2) {
			String s3 = param + s2;
		}
	}

	class Inner1 {
		public Inner1(int... param) {			
		}		
	}
	
	public void doit(final String param) {
		Object o = new Inner1Extension(new int[]{1, 2}, param);
	}
}
