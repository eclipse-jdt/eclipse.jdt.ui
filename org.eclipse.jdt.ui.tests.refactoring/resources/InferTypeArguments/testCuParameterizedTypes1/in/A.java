package p;

class A<T extends A<T>> {}
class B<T> extends A<B<T>> {}
