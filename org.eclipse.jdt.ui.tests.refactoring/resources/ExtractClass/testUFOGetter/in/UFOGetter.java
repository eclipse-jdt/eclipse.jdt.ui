package p;
public class UFOGetter {
        private int x;
        private int y;
        private int z;

        private String homePlanet;

        public UFOGetter(int x, int y, int z, 
                        String homePlanet) {
                this.x= x;
                this.y= y;
                this.z= z;
                this.homePlanet= homePlanet;
        }

        public String toString() {
                return "An UFO from " + homePlanet +
                        "is at position " +
                        "[" + x + ", " + y + ", " + z + "]";
        }
}