package p;

import java.io.File;
import java.net.MalformedURLException;

class F {
	public void foo() { 
		File file= null; 
		 
		try { 
			file.toURL(); 
		} catch (MalformedURLException e) { 
		} 
	} 
}