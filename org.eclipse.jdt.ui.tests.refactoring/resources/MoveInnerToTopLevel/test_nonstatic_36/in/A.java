package p;
class A
{
	public String bar2()
	{
		return "wee!";
	}
	public String bar3;
	
	public class Inner
	{
		
		public void bar0(){
			class Local{
				public void run()
				{
					System.out.println(bar2());
					bar3= "fred";
				}
			}
		}
		
		public void bar()
		{
			new Runnable()
			{
				public void run()
				{
					System.out.println(bar2());
					bar3= "fred";
				}
			};
		}
		
		class InnerInner{
			public void run()
			{
				System.out.println(bar2());
				bar3= "fred";
			}
		}
	}
}