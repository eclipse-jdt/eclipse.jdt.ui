package trycatch_out;
import java.io.File;
import java.net.MalformedURLException;

class TestExpression{	
	void fg(){
		File file= null;
		
		/*[*/int i;
		try {
			i = 3 * (2 + 1);
			file.toURL();/*]*/
		} catch (MalformedURLException e) {
		}
		i++;
	}
}
