package p;
class A{
	void f(){
		new Object(){
			void fx(){
				int i= s();
			}
			int s(){return 3;}
		};
	}
}