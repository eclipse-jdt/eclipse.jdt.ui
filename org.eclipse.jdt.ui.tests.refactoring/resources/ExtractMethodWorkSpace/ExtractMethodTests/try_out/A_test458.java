package try_out;

public class A_test458 {
	public void foo() throws Throwable{
		try{
			new A_test458();
		} catch (Throwable t){
			extracted(t);
		}
	}

	protected void extracted(Throwable t) throws Throwable {
		/*[*/throw t;/*]*/
	}
}
