//cannot rename to: newField
package p;
class A{
	void m(){
		int /*[*/i/*]*/ = 3;

		class Inner {
			public int newField;
			public void b() {
				newField = i;
			}
		}
	}
}