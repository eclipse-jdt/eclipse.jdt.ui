// 15, 7, 15, 15
package p;

import a.A;

class C
{
   b.A method()
   {
      return new b.A();
   }
   
   void failHere()
   {
      b.A foo= method(); //extract local variable here
   }
}