package p; //32, 16, 32, 45

public class A extends RelOptCost{
	public A(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		return super.computeSelfCost(a1, b1).multiplyBy(1);
	}

}

class A1 extends RelOptCost{
	public A1(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		return super.computeSelfCost(a1, b1).multiplyBy(9);
	}
}
class A2 extends RelOptCost{
	public A2(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		return super.computeSelfCost(a1, b1).multiplyBy(2);
	}
}
class A3 extends RelOptCost{
	public A3(int a, int b) {
		super(a, b);
	}

	@Override
	public RelOptCost computeSelfCost(int a1, int b1) {
		System.out.println(super.computeSelfCost(a1, b1));
		return super.computeSelfCost(a1, b1).multiplyBy(5);
	}
}

class RelOptCost {
	int a=0;
	int b=0;
	public RelOptCost(int a, int b) {
		this.a=a;
		this.b=b;
	}
	public RelOptCost computeSelfCost(int a1, int b1) {
		return new RelOptCost(a1,b1);
	}
	public RelOptCost multiplyBy(int d) {
		return new RelOptCost(this.a*d,this.b*d);
	}
}