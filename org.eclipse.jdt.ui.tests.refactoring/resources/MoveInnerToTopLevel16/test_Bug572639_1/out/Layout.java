package p;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// To see the error
// select Layout -> right click to get context menu, Refactor -> Move Type to a New File
// an extra import com.github.forax.tomahawk.schema.Schema.Layout.PrimitiveLayout; is in Layout.java
public interface Layout {
	public static Set<HashMap<Layout, HashSet<Layout.PrimitiveLayout>>> abc = null;  
    default boolean isPrimitive() {
      return this instanceof Layout.PrimitiveLayout;
    }
    record PrimitiveLayout() implements Layout { }
}