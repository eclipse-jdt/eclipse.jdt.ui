package trycatch_in;

public class TestThrowInCatch {
	void f() throws Exception{
		/*[*/try{
		} catch (Exception e){
			throw new Exception();
		}/*]*/
	}
}
