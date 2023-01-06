/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package inner;

public class Anon3 {
    final static String v15 = "Anon3";
    final static String v14 = "Anon3";

    void instanceMethod() {
        new Object() {
            final static String v15 = "Anon3$3";
            final static String v14 = "Anon3$15";
        };
        new Object() {
            final static String v15 = "Anon3$4";
            final static String v14 = "Anon3$16";
        };
    }

    Anon3() {
        new Object() {
            final static String v15 = "Anon3$5";
            final static String v14 = "Anon3$17";
        };
        new Object() {
            final static String v15 = "Anon3$6";
            final static String v14 = "Anon3$18";
        };
    }

    static void staticMethod() {
        new Object() {
            final static String v15 = "Anon3$7";
            final static String v14 = "Anon3$19";
        };
        new Object() {
            final static String v15 = "Anon3$8";
            final static String v14 = "Anon3$20";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon3$1";
            final static String v14 = "Anon3$1";
        };

        new Object() {
            final static String v15 = "Anon3$2";
            final static String v14 = "Anon3$2";
        };
    }

    class A2 {
        final static String v15 = "Anon3$A2";
        final static String v14 = "Anon3$A2";

        void instanceMethod() {
            new Object() {
                final static String v15 = "Anon3$A2$3";
                final static String v14 = "Anon3$5";
            };
            new Object() {
                final static String v15 = "Anon3$A2$4";
                final static String v14 = "Anon3$6";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon3$A2$1";
                final static String v14 = "Anon3$3";
            };
            new Object() {
                final static String v15 = "Anon3$A2$2";
                final static String v14 = "Anon3$4";
            };
        }
    }

    static class A3 {
        final static String v15 = "Anon3$A3";
        final static String v14 = "Anon3$A3";

        void instanceMethod() {
            new Object() {
                final static String v15 = "Anon3$A3$5";
                final static String v14 = "Anon3$11";
            };
            new Object() {
                final static String v15 = "Anon3$A3$6";
                final static String v14 = "Anon3$12";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon3$A3$1";
                final static String v14 = "Anon3$7";
            };
            new Object() {
                final static String v15 = "Anon3$A3$2";
                final static String v14 = "Anon3$8";
            };
        }

        static void staticMethod() {
            new Object() {
                final static String v15 = "Anon3$A3$7";
                final static String v14 = "Anon3$13";
            };
            new Object() {
                final static String v15 = "Anon3$A3$8";
                final static String v14 = "Anon3$14";
            };
        }

        static {
            new Object() {
                final static String v15 = "Anon3$A3$3";
                final static String v14 = "Anon3$9";
            };
            new Object() {
                final static String v15 = "Anon3$A3$4";
                final static String v14 = "Anon3$10";
            };
        }
    }
}
