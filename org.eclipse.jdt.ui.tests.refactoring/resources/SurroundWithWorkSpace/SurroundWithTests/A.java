import java.io.*;

class A{	
  private static InputStream input;

  public static void foo() {
	try {
		input = new FileInputStream("myfile");
	} catch (FileNotFoundException e) {
	}
  }
}
