import java.util.*;

class A_testLocalVarName_in {
	public static void main(String[] args) {
		Hashtable table = new Properties();
		Map map = table;
		table.put(table,table);
		table.containsKey(map);
	}
}
