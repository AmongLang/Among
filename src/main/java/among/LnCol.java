package among;

import java.util.Objects;

/**
 * Line and column. This object is for displaying purposes; as such, {@link LnCol#line} and {@link LnCol#column} starts
 * with {@code 1}, rather than {@code 0}.
 */
public final class LnCol{
	public final int line, column;

	public LnCol(int line, int column){
		this.line = line;
		this.column = column;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		LnCol lnCol = (LnCol)o;
		return line==lnCol.line&&column==lnCol.column;
	}
	@Override public int hashCode(){
		return Objects.hash(line, column);
	}

	@Override public String toString(){
		return line+":"+column;
	}
}
