import java.io.*;
public class X {
	void foo(final int out){
		new Object(){
			void bar(){
				/*START*/System.out.println(out)/*END*/;
			}
		};        
	}
}
