package ttmp.among.operator;

/**
 * Type of the operator
 */ // TODO docs
public enum OperatorType{
	BINARY, POSTFIX, PREFIX;

	public double defaultPriority(byte properties){
		switch(this){
			case BINARY: return (properties&OperatorProperty.RIGHT_ASSOCIATIVE)!=0 ?
					OperatorPriorities.BINARY_ASSIGN : OperatorPriorities.BINARY_CUSTOM;
			case POSTFIX: return OperatorPriorities.POSTFIX_CUSTOM;
			case PREFIX: default: return OperatorPriorities.PREFIX;
		}
	}

	@Override public String toString(){
		switch(this){
			case BINARY: return "binary";
			case POSTFIX: return "postfix";
			case PREFIX: return "prefix";
			default: throw new IllegalStateException("Unreachable");
		}
	}
}
