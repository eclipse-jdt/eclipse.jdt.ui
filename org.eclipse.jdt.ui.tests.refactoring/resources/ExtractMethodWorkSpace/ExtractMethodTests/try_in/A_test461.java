package try_in;

import java.io.IOException;

public class A_test461 {
	void f() throws IOException{
		/*[*/try{
			f();
		} catch (IOException e){
			throw new IOException();
		}/*]*/
	}
}
