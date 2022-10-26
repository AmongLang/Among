package ttmp.among.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Source stored as a codepoint array (which is just a fancy word for UTF-32 encoding (wait, I think this one is
 * fancier)), for faster access<br>
 * Also contains information about number of lines and start point.
 */
public final class Source{
	/**
	 * Creates new {@code Source} from {@code str}.
	 *
	 * @param src The source string
	 * @return New source
	 */
	public static Source of(String src){
		return new Source(src.split("\r\n?|\n"));
	}

	/**
	 * Creates new {@code Source} from strings read with {@code reader}. This method closes the reader.
	 *
	 * @param reader The reader to be used; it will be closed regardless of success or failure
	 * @return Source with strings read from {@code reader}
	 * @throws IOException          If an I/O error occurs
	 * @throws UncheckedIOException If an I/O error occurs(but it's unchecked(because java's checked exception system
	 *                              fucking sucks lmao))
	 */
	public static Source read(Reader reader) throws IOException{
		try(BufferedReader br = new BufferedReader(reader)){
			return new Source(br.lines().toArray(String[]::new));
		}
	}

	/**
	 * Special value indicating end of file.
	 */
	public static final int EOF = -1;

	/**
	 * Raw string source.
	 */
	private final String[] rawSource;
	/**
	 * Codepoints. The value directly corresponds to {@link Source#rawSource}, with line break('\n') inserted
	 * between each line.
	 */
	private final int[] codePoints;
	/**
	 * Positions of starting char of each {@link Source#rawSource}.
	 */
	private final int[] lineStarts;

	private Source(String[] rawSource){
		this.rawSource = rawSource;
		IntStream.Builder b = IntStream.builder();
		this.lineStarts = new int[rawSource.length];
		int position = 0;
		for(int i = 0; i<rawSource.length; i++){
			if(i!=0){
				b.accept('\n');
				position++;
			}
			lineStarts[i] = position;
			rawSource[i].codePoints().forEach(b);
			position += rawSource[i].codePointCount(0, rawSource[i].length());
		}
		this.codePoints = b.build().toArray();
	}

	public List<String> getRawSource(){
		return Collections.unmodifiableList(Arrays.asList(rawSource));
	}

	public int totalLength(){
		return codePoints.length;
	}
	public int totalLines(){
		return lineStarts.length;
	}

	/**
	 * @return Codepoint at the position, or {@link Source#EOF} if the position is outside the source's range.
	 * @throws ArrayIndexOutOfBoundsException If {@code position < 0}
	 */
	public int codePointAt(int position){
		return position<codePoints.length ? codePoints[position] : EOF;
	}

	public boolean isInBounds(int position){
		return position>=0&&position<codePoints.length;
	}

	/**
	 * @return Index of the line {@code position} is in part of.
	 * @throws IndexOutOfBoundsException If {@code position < 0}
	 */
	public int lineAt(int position){
		if(position<0) throw new IndexOutOfBoundsException("position");
		for(int i = 0; i<lineStarts.length; i++){
			int ls = lineStart(i);
			if(ls==position) return i;
			if(ls>position) return i-1;
		}
		return lineStarts.length-1;
	}
	public int lineStart(int line){
		return lineStarts[line];
	}
	public int lineEnd(int line){
		return lineStarts.length-1==line ? totalLength() : lineStarts[line+1]-1;
	}
	public int lineSize(int line){
		return lineEnd(line)-lineStart(line);
	}

	public LnCol getLnCol(int position){
		int l = lineAt(position);
		return new LnCol(l+1, position-lineStart(l)+1);
	}
}
