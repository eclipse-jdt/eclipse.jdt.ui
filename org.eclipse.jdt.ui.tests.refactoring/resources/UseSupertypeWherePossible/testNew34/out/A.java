package p;

//change to Vector
import java.util.Vector;

class A extends Vector {
    void foo() {
        Vector bar= null;
        System.out.println(bar.firstElement());
    }
}