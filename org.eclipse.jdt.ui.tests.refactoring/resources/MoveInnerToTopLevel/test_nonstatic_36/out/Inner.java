package p;
public class Inner
{
	
	/** Comment */
	private A a;

	/**
	 * @param a
	 */
	Inner(A a) {
		this.a= a;
	}

	public void bar0(){
		class Local{
			public void run()
			{
				System.out.println(Inner.this.a.bar2());
				Inner.this.a.bar3= "fred";
			}
		}
	}
	
	public void bar()
	{
		new Runnable()
		{
			public void run()
			{
				System.out.println(Inner.this.a.bar2());
				Inner.this.a.bar3= "fred";
			}
		};
	}
	
	class InnerInner{
		public void run()
		{
			System.out.println(Inner.this.a.bar2());
			Inner.this.a.bar3= "fred";
		}
	}
}