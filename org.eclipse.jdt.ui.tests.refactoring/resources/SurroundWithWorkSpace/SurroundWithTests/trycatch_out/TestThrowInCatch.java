package trycatch_out;

public class TestThrowInCatch {
	void f() throws Exception{
		try {
			/*[*/try{
			} catch (Exception e){
				throw new Exception();
			}/*]*/
		} catch (Exception e) {
		}
	}
}
