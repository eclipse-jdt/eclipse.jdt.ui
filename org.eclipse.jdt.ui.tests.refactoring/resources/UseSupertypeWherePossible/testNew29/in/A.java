package p;
//change to B
class B {
} 
class A extends B{
    void f(A a){
        A[] as= {a};
        as[0].f(null);
    }
}