package p;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Source {
    public String nonstatic1(String s, Target t) {
        return s + t.hashCode();
    }

}
