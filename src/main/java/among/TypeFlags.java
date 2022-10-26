package among;

import among.obj.Among;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags indicating type of the object.
 */
public interface TypeFlags{
	byte PRIMITIVE = 1;
	byte UNNAMED_OBJECT = 2;
	byte UNNAMED_LIST = 4;
	byte UNNAMED_OPERATION = 8;
	byte NAMED_OBJECT = 16;
	byte NAMED_LIST = 32;
	byte NAMED_OPERATION = 64;

	byte OBJECT = UNNAMED_OBJECT|NAMED_OBJECT;
	byte LIST = UNNAMED_LIST|NAMED_LIST;
	byte OPERATION = UNNAMED_OPERATION|NAMED_OPERATION;

	byte UNNAMED = UNNAMED_OBJECT|UNNAMED_LIST|UNNAMED_OPERATION;
	byte NAMED = NAMED_OBJECT|NAMED_LIST|NAMED_OPERATION;

	byte NAMEABLE = UNNAMED|NAMED;

	byte ANY = PRIMITIVE|NAMEABLE;

	static byte normalize(byte typeInference){
		return (byte)(typeInference&ANY);
	}

	static boolean matches(byte typeInference, Among value){
		return matches(typeInference, value, false);
	}
	static boolean matches(byte typeInference, Among value, boolean strictOperationCheck){
		return typeInference==ANY||(typeInference&from(value, strictOperationCheck))!=0;
	}

	static byte from(Among among){
		return from(among, false);
	}

	static byte from(Among among, boolean fuzzyOperation){
		if(among.isPrimitive()) return PRIMITIVE;
		else if(among.isObj()) return among.asNameable().hasName() ? NAMED_OBJECT : UNNAMED_OBJECT;
		else if(fuzzyOperation) return (byte)(among.asNameable().hasName() ? NAMED_OPERATION|NAMED_LIST : UNNAMED_OPERATION|UNNAMED_LIST);
		else if(among.asList().isOperation()) return among.asNameable().hasName() ? NAMED_OPERATION : UNNAMED_OPERATION;
		else return among.asNameable().hasName() ? NAMED_LIST : UNNAMED_LIST;
	}

	static String toString(byte flag){
		switch(flag&ANY){
			case ANY: return "Anything";
			case PRIMITIVE: return "Primitive";
			case UNNAMED_OBJECT: return "Unnamed Object";
			case UNNAMED_LIST: return "Unnamed List";
			case UNNAMED_OPERATION: return "Unnamed Operation";
			case NAMED_OBJECT: return "Named Object";
			case NAMED_LIST: return "Named List";
			case NAMED_OPERATION: return "Named Operation";
			case OBJECT: return "Object";
			case LIST: return "List";
			case OPERATION: return "Operation";
			case UNNAMED: return "Unnamed Collection";
			case NAMED: return "Named Collection";
			case NAMEABLE: return "Collection";
			default:{
				List<String> l = new ArrayList<>();
				if(has(flag, PRIMITIVE)) l.add("Primitive");

				if(has(flag, NAMEABLE)) l.add("Collection");
				else{
					if(has(flag, NAMED)){
						l.add("Named Collection");
						flag ^= NAMED;
					}else if(has(flag, UNNAMED)){
						l.add("Unnamed Collection");
						flag ^= UNNAMED;
					}

					if(has(flag, OBJECT)) l.add("Object");
					else if(has(flag, UNNAMED_OBJECT)) l.add("Unnamed Object");
					else if(has(flag, NAMED_OBJECT)) l.add("Named Object");
					if(has(flag, LIST)) l.add("List");
					else if(has(flag, UNNAMED_LIST)) l.add("Unnamed List");
					else if(has(flag, NAMED_LIST)) l.add("Named List");
					if(has(flag, OPERATION)) l.add("Operation");
					else if(has(flag, UNNAMED_OPERATION)) l.add("Unnamed Operation");
					else if(has(flag, NAMED_OPERATION)) l.add("Named Operation");
				}
				switch(l.size()){
					case 0: return "Invalid";
					case 1: return l.get(0);
					default: return String.join(" or ", l);
				}
			}
		}
	}

	static boolean has(byte flag, byte value){
		return (flag&value)==value;
	}
}
