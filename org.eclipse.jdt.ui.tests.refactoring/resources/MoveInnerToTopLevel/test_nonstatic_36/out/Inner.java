package p;
public class Inner
{
	private A a;
	/**
	 * @param A
	 */
	Inner(A a) {
		this.a= a;
		// TODO Auto-generated constructor stub
	}
	
	public void bar()
	{
		new Runnable()
		{
			public void run()
			{
				System.out.println(Inner.this.a.bar2());
			}
		};
	}
}