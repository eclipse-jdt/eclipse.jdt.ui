package p;
//name clash in subclass
class A{
	int m(int i){
	}
}
class B extends A{
	int m(int x){
	}
}