package try_in;

import java.io.IOException;

public class A_test462 {
	void f() throws IOException{
		/*[*/try{
			f();
		} catch (IOException e){
		} finally {
			throw new IOException();
		}/*]*/
	}
}
