package import_out;

import import_use.List;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class Provider {
	public File useAsReturn() {
		return null;
	}
	public void useInArgument(File file) {
		file= null;
	}
	public void useInDecl() {
		List list= null;
	}
	public int useInDecl2(){
	  return new ArrayList().size();
	}	
	public Object useInDecl3(){
	  return new java.util.HashMap();
	}	
	public void useInClassLiteral() {
		Class clazz= File.class;
	}
	public void useArray() {
		List[] lists= null;
	}
	public void useInLocalClass() {
		class Local extends File implements Comparable {
			public Local(String s) {
				super(s);
			}
			public void foo(Map map) {
			}
			public int compareTo(Object o) {
				return 0;
			}
		}
	}
}
