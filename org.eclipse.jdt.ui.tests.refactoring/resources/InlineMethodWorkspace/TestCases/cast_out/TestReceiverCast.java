package cast_out;

public class TestReceiverCast {
	private void foo(Object obj){
		String s= ((String)obj).intern();
	}

	private static String goo(String string){
		return string.intern();
	}
}
