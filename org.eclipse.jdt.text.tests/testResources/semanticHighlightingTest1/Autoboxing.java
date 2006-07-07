
class Autoboxing {
    void m(int pi, Integer pBigI) {
        int i= pBigI; // vardeclFragment
        Number bigN= i; // vardeclFragment
        Integer bigI= null;

        m(i, bigI); // method invocation
        m(bigI, bigI); // method invocation

        m(bigI, m(bigI, i)); // nested method invocation
        m(bigI, m(i, bigI)); // nested method invocation
        m(bigI, foo()); // method return value

        bigN= pBigI; // assignment to Number
        bigN= pi; // assignment
        bigN= i; // assignment

        bigI= pBigI; // assignment to Integer
        bigI= pi; // assignment
        bigI= i; // assignment

        i= pBigI; // assignment to int
        i= i + 1;
        i= pBigI + i;

        i= true ? pBigI + i : bigI + (i + pBigI); // conditionals and parenthesized expr

        bigI= true ? pBigI + i : i + pBigI; // conditionals
        bigI= true ? pBigI : i;
        bigI= true ? i : pBigI;

        int[] array= new int[bigI]; // array creation

        array[i]= bigI; // array assignment
        array[bigI]= i; // array access

        bigI= array[bigI + i]; // infix expression

        for (int index= bigI; index < pBigI; index+= bigI) {
            // var decl fragments, infix comparisons
        }

        int bar= bigI, foo= pBigI; // multi var declarations

        i= -bigI; // prefix expr
        i= ~bigI;
        bigI= -42;

        bigI= foo(); // method return value

        if (foo() == number()) // comparison
            return;
    }

    int foo() {
        return 0;
    }

    Integer number() {
        return null;
    }

}
