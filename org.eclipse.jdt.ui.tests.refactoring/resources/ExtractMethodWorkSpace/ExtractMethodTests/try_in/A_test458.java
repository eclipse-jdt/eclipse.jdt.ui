package try_in;

public class A_test458 {
	public void foo() throws Throwable{
		try{
			new A_test458();
		} catch (Throwable t){
			/*[*/throw t;/*]*/
		}
	}
}
