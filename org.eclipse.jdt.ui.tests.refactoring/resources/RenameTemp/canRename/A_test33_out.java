//renaming to b, no ref update
package p;
class A{
   A A;
   A m(A a){
   	A /*[*/b/*]*/= null;
     A:
        for (;;){
          if (A.m(A)==A)
             break A;
        }
      return A;
   };
}