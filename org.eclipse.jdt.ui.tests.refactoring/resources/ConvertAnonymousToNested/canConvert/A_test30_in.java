package p;
public class A {
    void m() {
        Runnable greeter= new Runnable() {
            public void run() {
                System.out.println("Hello World")
            }
        };
        greeter.run();
    
}