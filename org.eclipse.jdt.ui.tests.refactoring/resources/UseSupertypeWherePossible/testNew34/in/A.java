package p;

//change to Vector
import java.util.Vector;

class A extends Vector {
    void foo() {
        A bar= null;
        System.out.println(bar.firstElement());
    }
}