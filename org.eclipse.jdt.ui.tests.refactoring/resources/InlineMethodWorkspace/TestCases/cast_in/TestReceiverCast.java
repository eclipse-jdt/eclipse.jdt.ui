package cast_in;

public class TestReceiverCast {
	private void foo(Object obj){
		String s= /*]*/goo/*[*/((String)obj);
	}

	private static String goo(String string){
		return string.intern();
	}
}
