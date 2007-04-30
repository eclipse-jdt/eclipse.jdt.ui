package p;

public class TestInterfaceMethod implements ITestInterfaceMethod {
	/* (non-Javadoc)
	 * @see p.ITestInterfaceMethod#foo(java.lang.String, int, double)
	 */
	public void foo(String id, int param, double blubb){
		foo(id,param,blubb);
	}
}
