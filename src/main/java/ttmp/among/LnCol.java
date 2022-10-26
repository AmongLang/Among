package ttmp.among;

import java.util.Objects;

/**
 * Line & Column
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
