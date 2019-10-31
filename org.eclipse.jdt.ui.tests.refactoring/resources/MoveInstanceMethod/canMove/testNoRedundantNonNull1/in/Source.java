package p;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Source {
    public String nonstatic1(String s, Target t) {
        return s + t.hashCode();
    }

    public String nonstatic2(String s, Target.Nested t) {
        return s + t.hashCode();
    }

}
