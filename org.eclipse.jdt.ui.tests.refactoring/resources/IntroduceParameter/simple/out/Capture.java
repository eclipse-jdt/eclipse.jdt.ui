//selection: 10, 12, 10, 13
//name: list -> arg2
package capture;

import java.util.List;

public class Capture {
    List<?> b= null;
    void take(List<?> a, List<?> arg2) {
        a= arg2;
    }
}
