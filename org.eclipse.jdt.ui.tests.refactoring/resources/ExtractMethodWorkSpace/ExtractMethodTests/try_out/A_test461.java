package try_out;

import java.io.IOException;

public class A_test461 {
	void f() throws IOException{
		extracted();
	}

	protected void extracted() throws IOException {
		/*[*/try{
			f();
		} catch (IOException e){
			throw new IOException();
		}/*]*/
	}
}
