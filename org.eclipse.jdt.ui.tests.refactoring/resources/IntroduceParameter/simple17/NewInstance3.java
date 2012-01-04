//selection: 11, 33, 11, 48
//name: hashMap -> hashMap
package simple;

import java.util.Map;
import java.util.HashMap;


public class Diamond {
    Map<?,?> test() {
        Map<String, String> m = new HashMap<>();
		return m;        
    }

    public static void test1(String[] args) {
        new Diamond().test();
    }   
    
    public static Map<?, ?> test2(Diamond a) {
        Map<?, ?> bar = a.test();
       return bar;
    }
    
    int test3(int a){
		test();
    	return a;    	
    }
}
