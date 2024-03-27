package invalidSelection;

public class A_test3001 {
	public int calculateSum(int[] numbers) {
        int sum = 0;
        try {
        	 /*]*/for (int i = 0; i < numbers.length; i++) {
                if (numbers[i] < 0) {
                    
                    throw new Exception();
                }
                sum += numbers[i];
            }/*[*/
        } catch (Exception e) {
            return sum;
        }
        return sum;
    }


    public int test() {
        int[] numbers = {1, 2, -3, 4, 5};
        int result = calculateSum(numbers);
        System.out.println("Sum: " + result);
        return result;
    }

    public static void main(String[] args) {
    	A_test3001 b = new A_test3001();
        b.test();
    }
}

