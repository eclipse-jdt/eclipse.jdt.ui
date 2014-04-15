package enum_out;

public class Test_416198 {

	public static int i;
	
	enum E{
		D{
			void getEnum(){
				int i= Test_416198.i;
			}
		}, 
		R(Test_416198.i);
		
		E(){
		}
		
		E(int count){
			count= Test_416198.i;
		}
	}
	
	interface I{
		int total = Test_416198.i;
	}
	
	class C{
		public void foo(){
			bar(Test_416198.i);
		}

		private void bar(int ai) {}
	}
}
