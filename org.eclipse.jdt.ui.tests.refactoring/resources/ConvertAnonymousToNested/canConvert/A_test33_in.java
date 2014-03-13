package p;
public interface A{
   <T> void a();
   <T> void b();
}

class Z{
   void m(){
      A a = new A(){ 
         public <T> void b(){}
         public <T> void a(){}
      };
   }
}
