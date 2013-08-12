package p;
public interface A{
   <T> void a();
   <T> void b();
}

class Z{
   private static final class AImpl implements A {
		public <T> void b(){}
		public <T> void a(){}
	}

void m(){
      A a = new AImpl();
   }
}
