package p;
class A{
	void f(){
		new Object(){
			private int i= s();
			void fx(){
			}
			int s(){return 3;}
		};
	}
}