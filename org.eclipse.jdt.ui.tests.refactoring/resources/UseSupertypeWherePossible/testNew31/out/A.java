package p;
//change to B
class B {
} 
class A extends B{
    void f(){
        A[] as= new A[0];
        as[0].f();
    }
    void fz(){}
}