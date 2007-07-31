package p;
public class UFOGetter {
        private Position position = new Position();
		private String homePlanet;

        public UFOGetter(int x, int y, int z, 
                        String homePlanet) {
                this.position.setX(x);
                this.position.setY(y);
                this.position.setZ(z);
                this.homePlanet= homePlanet;
        }

        public String toString() {
                return "An UFO from " + homePlanet +
                        "is at position " +
                        "[" + position.getX() + ", " + position.getY() + ", " + position.getZ() + "]";
        }
}