package p;
class A extends Exception{
 void m() throws A{
 }
	void j(){
 	try{
 		m();
 	}
 	catch (A a){
 	}
 }
	
}
