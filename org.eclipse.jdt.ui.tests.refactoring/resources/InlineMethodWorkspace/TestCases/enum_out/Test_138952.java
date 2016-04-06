package enum_in;

class Infomat {

    public enum Fruit { 
	    APPLE, BANANA, DATE; 
    }
	
    public static String getTaste(Fruit fruit) {
	    switch(fruit) {
	        case APPLE	: return "yammy";
	        case BANANA : return "ok";
	        case DATE 	: return "very yammy";
	        default:	return "never eat that one..";
        }
    }
	
    public String getInstanceTaste(Fruit f) {
	    switch(f) {
		    case APPLE	: return "yammy";
		    case BANANA : return "ok";
		    case DATE 	: return "very yammy";
		    default:	return "never eat that one..";
		}
    }

}