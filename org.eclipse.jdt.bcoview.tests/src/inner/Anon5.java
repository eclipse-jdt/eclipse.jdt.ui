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

public class Anon5 {
    final static String v15 = "Anon5";
    final static String v14 = "Anon5";

    void instanceMethod1() {
        new Object() {
            final static String v15 = "Anon5$5";
            final static String v14 = "Anon5$10";
        };

        new Object() {
            final static String v15 = "Anon5$6";
            final static String v14 = "Anon5$11";

            {
                new Object() {
                    final static String v15 = "Anon5$6$1";
                    final static String v14 = "Anon5$12";
                };
            }
        };

        new Object() {
            final static String v15 = "Anon5$7";
            final static String v14 = "Anon5$13";
        };
    }

    {
        new Object() {
            final static String v15 = "Anon5$1";
            final static String v14 = "Anon5$1";
        };
    }

    Anon5(){
        new Object() {
            final static String v15 = "Anon5$8";
            final static String v14 = "Anon5$14";
        };
    }

    static {
        new Object() {
            final static String v15 = "Anon5$2";
            final static String v14 = "Anon5$2";
        };
    }

    {
        new Object() {
            final static String v15 = "Anon5$3";
            final static String v14 = "Anon5$3";
        };
    }


    static {
        new Object() {
            final static String v15 = "Anon5$4";
            final static String v14 = "Anon5$4";
        };
    }



    void instanceMethod2() {
        class A3 {
            final static String v15 = "Anon5$1A3";
            final static String v14 = "Anon5$1$A3";

            {
                new Object() {
                    final static String v15 = "Anon5$1A3$1";
                    final static String v14 = "Anon5$15";
                };
            }
        }

    }

    class A1 {
        final static String v15 = "Anon5$A1";
        final static String v14 = "Anon5$A1";

        A1(){
            new Object() {
                final static String v15 = "Anon5$A1$2";
                final static String v14 = "Anon5$6";
            };
        }
        {
            new Object() {
                final static String v15 = "Anon5$A1$1";
                final static String v14 = "Anon5$5";
            };
        }

    }

    static class A2 {
        final static String v15 = "Anon5$A2";
        final static String v14 = "Anon5$A2";

        A2(){
            new Object() {
                final static String v15 = "Anon5$A2$3";
                final static String v14 = "Anon5$9";
            };
        }

        static {
            new Object() {
                final static String v15 = "Anon5$A2$1";
                final static String v14 = "Anon5$7";
            };
        }

        {
            new Object() {
                final static String v15 = "Anon5$A2$2";
                final static String v14 = "Anon5$8";
            };
        }
    }
}
