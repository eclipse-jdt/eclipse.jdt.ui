package p;
class A{
    class Inner{}
    void f(){
        new A(){
            Object x= new Object();
        };
    }
}