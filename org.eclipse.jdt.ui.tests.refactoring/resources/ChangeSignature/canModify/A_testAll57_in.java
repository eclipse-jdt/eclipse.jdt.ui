package p; //swap hello and goodbye

class TEST
{
   public interface X
   {
	  public void method(final int i, String hello, final String goodbye);
   }
   
   private static X x = new X()
	{
	   public void method(final int i, String hello, final String goodbye)
	   {
		  System.err.println(hello + goodbye);
	   }
	};
   
   public static void main(String[] args)
   {
	  x.method(1, "hello", "goodbye");
   }
}
