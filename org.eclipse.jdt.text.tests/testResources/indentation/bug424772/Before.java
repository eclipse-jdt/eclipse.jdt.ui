package indentbug;

public class Bug {
	void t1()
			throws A1.A2, Exception
	{

	}

	String t2()[] 
			throws Exception
	{
		return new String[0];
	}

	String t3(int i,
			int j)
			[] []
	{
		return new String[0][0];
	}
}

class A1  { 
	class A2 extends Exception {

	}
}