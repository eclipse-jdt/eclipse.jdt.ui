package p;

public class A {
	static void takeP(Class c) {}
	static void takePQ(Class c) {}
	
	class P {
		{
			Class p= getClass();
			takeP(p);
			takePQ(p);
		}
	}
	
	class Q {
		{
			Class q= getClass();
			takePQ(q);
		}
	}
	
	class Z {
		{
			Class z= getClass();
		}
	}
}
