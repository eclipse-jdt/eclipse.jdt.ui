package p;
class A {
	public static void foo1(){
		A foo = new A()	{
			public void foo(){
				A foo = new A(){};
			}
		};
	}
}
