package among.internals;

enum OperatorPropertyEnum{
	LEFT_ASSOCIATIVE, RIGHT_ASSOCIATIVE, ACCESSOR;

	@Override public String toString(){
		switch(this){
			case LEFT_ASSOCIATIVE: return "left-associative";
			case RIGHT_ASSOCIATIVE: return "right-associative";
			case ACCESSOR: return "accessor";
			default: throw new IllegalStateException("Unreachable");
		}
	}
}
