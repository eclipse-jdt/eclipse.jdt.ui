package p;
class A{
	void f(){
		new Object(){
			private int i;

			void fx(){
				i= 0;
			}	
		};
	}
}