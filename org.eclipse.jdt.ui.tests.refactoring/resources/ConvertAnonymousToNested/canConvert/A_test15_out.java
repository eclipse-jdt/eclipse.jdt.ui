package p;

import java.util.ArrayList;

class A{
	private static final class Inner extends ArrayList{
		private Inner(int arg0){
			super(arg0);
		}
	}
    void g(){
        new Inner(6);
    }
}