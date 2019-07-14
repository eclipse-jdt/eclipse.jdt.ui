//inlining would introduce compile error (method call on null)
package p;
class A{
	String m(){
		Integer x= null;
		return x.toString();
	}
}