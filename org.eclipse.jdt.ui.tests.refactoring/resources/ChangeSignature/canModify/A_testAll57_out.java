package p; //swap hello and goodbye

class TEST
{
   public interface X
   {
	  public void method(final int i, final String goodbye, String hello);
   }
   
   private static X x = new X()
	{
	   public void method(final int i, final String goodbye, String hello)
	   {
		  System.err.println(hello + goodbye);
	   }
	};
   
   public static void main(String[] args)
   {
	  x.method(1, "goodbye", "hello");
   }
}
