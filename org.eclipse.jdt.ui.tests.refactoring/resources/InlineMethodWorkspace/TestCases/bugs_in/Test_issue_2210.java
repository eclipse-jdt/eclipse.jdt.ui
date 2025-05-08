package bugs_in;

public class Test_issue_2210 {
	enum EntityType{X}
	static class Case extends Bug{}
	static class Leaf extends Bug{}
	static class CaseNote extends Leaf{}
	static class SuperView extends Leaf{}
	static class CaseEntity extends Leaf{
		EntityType getEntityType(){return null;}
	}
	public static void main(String...args){
		System.out.println(fetch(new Case()));
	}
	static String /*]*/getId/*[*/(final Leaf leaf){ // LINE 13
		return leaf instanceof CaseNote?"a":
	       leaf instanceof CaseEntity?
		   editorId(((CaseEntity)leaf).
		   getEntityType()):"b";
	}
	static String editorId(final EntityType type){
		return""+type;
	}
	static String fetch(final Bug record){
		return record instanceof SuperView?"c":
			   record instanceof Case?
			   "DONE!":getId((Leaf)record);
	}
}
