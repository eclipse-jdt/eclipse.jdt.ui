class A {
    private StringBuffer buf;
    
    public void writeToBuffer() {
        buf.append("xyzzy");
        //--------cut here
        buf.append("plugh");
        buf.append("plover");
        //--------cut here
        buf.append("samoht");
    }
}