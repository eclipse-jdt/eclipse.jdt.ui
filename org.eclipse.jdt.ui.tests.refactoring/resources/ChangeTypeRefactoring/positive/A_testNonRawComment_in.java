import java.util.*;

class A_testNonRawComment_in {
	<T> void  foo(){
		  Set<String>/*blah*/ x = new HashSet<String>();
		  x.add("");
		}
}
