package p;
//renaming A.m to k
class A{
	static void k(){
	}
}
class test{
	void m(){
		class X extends A{
			void m(){
		}
	}
}