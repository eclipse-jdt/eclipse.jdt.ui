package p;
class Test1 {
		public static void main(String[] args) {
				StringBuffer    buf = new StringBuffer(16);

				buf.append("Args:");
				for(int i=0; i < args.length; i++)
						buf.append(" '")
						   .append(args[i])
						   .append("'");
				System.out.println(buf.toString());
		}
}
