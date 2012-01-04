//selection: 11, 33, 11, 48
//name: hashMap -> hashMap
package simple;

import java.util.Map;
import java.util.HashMap;


public class Diamond {
    Map<?,?> test(HashMap<String, String> hashMap) {
        Map<String, String> m = hashMap;
		return m;        
    }

    public static void test1(String[] args) {
        new Diamond().test(new HashMap<String, String>());
    }   
    
    public static Map<?, ?> test2(Diamond a) {
        Map<?, ?> bar = a.test(new HashMap<String, String>());
       return bar;
    }
    
    int test3(int a){
		test(new HashMap<String, String>());
    	return a;    	
    }
}
