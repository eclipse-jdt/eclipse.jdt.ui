import java.util.*;

class A_testNonRawComment_in {
	<T> void  foo(){
		  Collection<String>/*blah*/ x = new HashSet<String>();
		  x.add("");
		}
}
