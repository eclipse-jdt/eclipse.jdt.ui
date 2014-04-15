package enum_in;

public class Test_416198 {

	public static int i;
	
	public static int /*]*/getAi()/*[*/{
		return i;
	}
	
	enum E{
		D{
			void getEnum(){
				int i= Test_416198.getAi();
			}
		}, 
		R(Test_416198.getAi());
		
		E(){
		}
		
		E(int count){
			count= Test_416198.getAi();
		}
	}
	
	interface I{
		int total = Test_416198.getAi();
	}
	
	class C{
		public void foo(){
			bar(Test_416198.getAi());
		}

		private void bar(int ai) {}
	}
}
