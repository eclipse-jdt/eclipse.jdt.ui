import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

class A_testLocalVarDecl_in {
	public static void main(String[] args) {
		Hashtable table = new Properties();
		Map map = table;
		table.put(table,table);
		table.containsKey(map);
		map = new HashMap();
	}
}
