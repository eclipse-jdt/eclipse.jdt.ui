//inlining would introduce compile error (overloaded method ambiguity)
package p;
class A{
	void m(){
		String x= null;
		System.out.println(x);
	}
}