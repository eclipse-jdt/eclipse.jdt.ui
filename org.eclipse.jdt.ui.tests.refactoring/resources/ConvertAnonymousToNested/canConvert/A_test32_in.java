package p;
public class A {

	class Inner1 {
		public Inner1(int... param) {			
		}		
	}
	
	public void doit(final String param) {
		Object o = new Inner1(1, 2) {
			public void m1(String s2) {
				String s3 = param + s2;
			}
		};
	}
}
