package among.internals;

import among.TypeFlags;

final class ParserTypeInference{
	final int index;
	byte type = TypeFlags.ANY;

	ParserTypeInference(int index){
		this.index = index;
	}
}
