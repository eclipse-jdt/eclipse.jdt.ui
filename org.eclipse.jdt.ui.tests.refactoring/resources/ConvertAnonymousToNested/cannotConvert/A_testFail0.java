package p;
//references a local type
class A{
    void f(){
        class Local{}
        new A(){
            Object x= new Local();
        };
    }
}