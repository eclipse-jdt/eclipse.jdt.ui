package trycatch_out;

public class TestThenStatement {
	
 void foo() {
   TestThenStatement bar= null;
   if (bar != null)
	try {
		bar.run();
	} catch (InterruptedException e) {
	}/*[*/
 }
 
 void run() throws InterruptedException{
 }
}