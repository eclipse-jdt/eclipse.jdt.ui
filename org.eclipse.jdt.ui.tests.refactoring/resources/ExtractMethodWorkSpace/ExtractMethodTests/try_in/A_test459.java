package try_in;

public class A_test459 {
	public abstract class Protectable {
		public abstract void protect() throws Exception;
		public void setUp() throws Exception {
		}
	}
	public void foo() {
		/*[*/Protectable p= new Protectable() {
			public void protect() throws Exception {
				setUp();
			}
		};/*]*/
	}
}
