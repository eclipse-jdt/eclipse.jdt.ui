package p;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Source {
    public String nonstatic2(String s, Target.Nested t) {
        return s + t.hashCode();
    }

}
