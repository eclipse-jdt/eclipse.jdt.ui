package p;
//static disabled
class A{
    void f(){
        new A(){
            void g(){
                f();
            }
        };
    }
}