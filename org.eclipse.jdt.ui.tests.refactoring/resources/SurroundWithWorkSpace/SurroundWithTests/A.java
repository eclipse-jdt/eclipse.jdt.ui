import java.io.File;
import java.net.URL;

class A {
	public void foo() {
		File file= null;
		
		URL url= file.toURL();
		
		url= null;
	}
}