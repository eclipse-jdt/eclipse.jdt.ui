package p;
class A
{
	public String bar2()
	{
		return "wee!";
	}
	public class Inner
	{
		
		public void bar()
		{
			new Runnable()
			{
				public void run()
				{
					System.out.println(bar2());
				}
			};
		}
	}
}