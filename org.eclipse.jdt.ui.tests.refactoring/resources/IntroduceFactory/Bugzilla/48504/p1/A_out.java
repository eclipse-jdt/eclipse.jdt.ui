package p;

public class A {
    {
        field= 10;
    }
    
    protected /*[*/A/*]*/() {
        
    }
    
    private int field;
    
    static class XX extends A {
        public void foo() {
            bar();
        }
        public void bar() {
        }
    }
    public void foo(int y) {
        Runnable runnable= new Runnable() {
            private int field;
            public void run() {
                {
                    A a= null;
                }
            }
        };
    }
    
    public String foo(String ss) {
        A a= B.createA();
        return ss;
    }
}
