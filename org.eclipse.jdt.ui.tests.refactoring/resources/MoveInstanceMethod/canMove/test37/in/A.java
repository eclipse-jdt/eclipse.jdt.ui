package p;
class A {
    B destination = new B();
    public void methodToMoveToDestination(String value) {
        this.destination.value = value;
        destination.value = value;
    }
}