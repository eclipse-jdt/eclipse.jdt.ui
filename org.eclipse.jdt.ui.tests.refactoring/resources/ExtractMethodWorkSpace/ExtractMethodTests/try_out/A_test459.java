package try_out;

public class A_test459 {
	public abstract class Protectable {
		public abstract void protect() throws Exception;
		public void setUp() throws Exception {
		}
	}
	public void foo() {
		extracted();
	}
	protected void extracted() {
		/*[*/Protectable p= new Protectable() {
			public void protect() throws Exception {
				setUp();
			}
		};/*]*/
	}
}
