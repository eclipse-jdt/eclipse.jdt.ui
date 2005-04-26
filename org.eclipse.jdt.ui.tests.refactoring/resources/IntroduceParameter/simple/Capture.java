//selection: 10, 12, 10, 13
//name: name -> arg2
package simple;

import java.util.List;

public class Capture {
    List<?> b= null;
    void take(List<?> a) {
        a= b;
    }
}
