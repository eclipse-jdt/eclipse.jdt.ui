package try_out;

import java.io.IOException;

public class A_test462 {
	void f() throws IOException{
		extracted();
	}

	protected void extracted() throws IOException {
		/*[*/try{
			f();
		} catch (IOException e){
		} finally {
			throw new IOException();
		}/*]*/
	}
}
