package among;

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
	 * @throws IOException If an I/O error occurs
	 */
	public static Source read(Reader reader) throws IOException{
		try(BufferedReader br = new BufferedReader(reader)){
			return new Source(br.lines().toArray(String[]::new));
		}catch(UncheckedIOException ex){
			throw ex.getCause();
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
	 * Returns codepoint at given position. If {@code position} is greater than {@link Source#totalLength()}, {@link
	 * Source#EOF} is returned.
	 *
	 * @param position Codepoint index
	 * @return Codepoint at {@code position}, or {@link Source#EOF} if the position is outside the source's range
	 * @throws ArrayIndexOutOfBoundsException If {@code position < 0}
	 */
	public int codePointAt(int position){
		return position<codePoints.length ? codePoints[position] : EOF;
	}

	/**
	 * Returns whether {@code position} is in range of valid codepoint index for this source; i.e. {@code 0} to {@link
	 * Source#totalLines()}.
	 *
	 * @param position Codepoint index
	 * @return Whether {@code position} is in range of valid codepoint index for this source
	 */
	public boolean isInBounds(int position){
		return position>=0&&position<codePoints.length;
	}

	/**
	 * Returns the index of the line {@code position} is in part of. If {@code position} is greater than {@link
	 * Source#totalLength()}, index of last line is returned.
	 *
	 * @param position Codepoint index
	 * @return Index of the line {@code position} is in part of
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
	/**
	 * Returns start of the line. Returned position is aligned with first character of the line. If the line
	 * is empty, the position will be aligned with newline character instead.
	 *
	 * @param line Line index
	 * @return Start of the line
	 * @throws ArrayIndexOutOfBoundsException If {@code line < 0 || line >= totalLines() }
	 */
	public int lineStart(int line){
		return lineStarts[line];
	}
	/**
	 * Returns end of the line. Returned position is aligned with newline character of the line. If the line
	 * is the last line, {@link Source#totalLength()} will be returned instead.
	 *
	 * @param line Line index
	 * @return End of the line
	 * @throws ArrayIndexOutOfBoundsException If {@code line < 0 || line >= totalLines() }
	 */
	public int lineEnd(int line){
		return lineStarts.length-1==line ? totalLength() : lineStarts[line+1]-1;
	}
	/**
	 * Returns size of the line, excluding newline characters and such.
	 *
	 * @param line Line index
	 * @return Size of the line
	 * @throws ArrayIndexOutOfBoundsException If {@code line < 0 || line >= totalLines() }
	 */
	public int lineSize(int line){
		return lineEnd(line)-lineStart(line);
	}

	/**
	 * Returns line and column at given position.
	 *
	 * @param position Codepoint index
	 * @return Line and column at given position
	 * @throws IndexOutOfBoundsException If {@code position < 0}
	 */
	public LnCol getLnCol(int position){
		int l = lineAt(position);
		return new LnCol(l+1, position-lineStart(l)+1);
	}
}
