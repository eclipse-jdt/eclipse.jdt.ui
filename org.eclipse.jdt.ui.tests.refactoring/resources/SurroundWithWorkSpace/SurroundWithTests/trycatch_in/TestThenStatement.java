package trycatch_in;

public class TestThenStatement {
	
 void foo() {
   TestThenStatement bar= null;
   if (bar != null)
	 /*]*/bar.run();/*[*/
 }
 
 void run() throws InterruptedException{
 }
}