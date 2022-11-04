package among.report;

import among.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Implementation of {@link ReportHandler} that stores all reports as list of {@link Report}s.
 */
public class ReportList extends AbstractList<Report> implements ReportHandler, RandomAccess{
	protected final List<Report> reports = new ArrayList<>();

	public ReportList(){}
	public ReportList(Collection<Report> other){
		this.reports.addAll(other);
	}

	/**
	 * Whether there are any errors reported or not.
	 *
	 * @return Whether there are any errors reported or not
	 */
	public boolean hasError(){
		for(Report r : reports)
			if(r.type()==ReportType.ERROR) return true;
		return false;
	}

	/**
	 * Whether there are any warnings reported or not.
	 *
	 * @return Whether there are any warnings reported or not
	 */
	public boolean hasWarning(){
		for(Report r : reports)
			if(r.type()==ReportType.WARN) return true;
		return false;
	}

	public void printReports(){
		printReports(null, null);
	}
	public void printReports(@Nullable Source source){
		printReports(null, source);
	}
	public void printReports(@Nullable String path){
		printReports(path, null);
	}
	public void printReports(@Nullable String path, @Nullable Source source){
		if(reports.isEmpty()) return;
		printReports(path, source, hasError() ? System.err::println : System.out::println);
	}

	public void printReports(@Nullable String path, @Nullable Source source, Consumer<String> logger){
		if(reports.isEmpty()) return;
		int infoCount = 0;
		int warningCount = 0;
		int errorCount = 0;
		for(Report r : reports){
			switch(r.type()){
				case INFO:
					infoCount++;
					break;
				case WARN:
					warningCount++;
					break;
				case ERROR:
					errorCount++;
					break;
			}
		}
		List<String> types = new ArrayList<>();
		if(errorCount>0) types.add(errorCount==1 ? "1 error" : errorCount+" errors");
		if(warningCount>0) types.add(warningCount==1 ? "1 warning" : warningCount+" warnings");
		if(infoCount>0) types.add(infoCount+" info");

		StringBuilder stb = new StringBuilder();
		if(path==null) stb.append("Compilation finished with ");
		else stb.append("Compilation of script at '").append(path).append("' finished with ");
		for(int i = 0; i<types.size(); i++){
			if(i>0) stb.append(i==types.size()-1 ? " and " : ", ");
			stb.append(types.get(i));
		}

		logger.accept(stb.toString());
		for(Report report : this.reports)
			report.print(source, logger);
	}

	@Override public void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints){
		reports.add(new Report(type, message, srcIndex, ex, hints));
	}

	@Override public int size(){return reports.size();}
	@Override public boolean isEmpty(){return reports.isEmpty();}
	@Override public boolean contains(Object o){return reports.contains(o);}
	@NotNull @Override public Object[] toArray(){return reports.toArray();}
	@SuppressWarnings("SuspiciousToArrayCall") @NotNull @Override public <T> T[] toArray(@NotNull T[] a){return reports.toArray(a);}
	@Override public boolean containsAll(@NotNull Collection<?> c){return reports.containsAll(c);}
	@Override public void clear(){reports.clear();}
	@Override public Report get(int index){return reports.get(index);}
	@Override public int indexOf(Object o){return reports.indexOf(o);}
	@Override public int lastIndexOf(Object o){return reports.lastIndexOf(o);}
	@Override public Spliterator<Report> spliterator(){return reports.spliterator();}
	@Override public Stream<Report> stream(){return reports.stream();}
	@Override public Stream<Report> parallelStream(){return reports.parallelStream();}
	@Override public void forEach(Consumer<? super Report> action){reports.forEach(action);}

	@Override public Iterator<Report> iterator(){return Collections.unmodifiableList(reports).iterator();}
	@Override public ListIterator<Report> listIterator(){return Collections.unmodifiableList(reports).listIterator();}
	@Override public ListIterator<Report> listIterator(int index){return Collections.unmodifiableList(reports).listIterator(index);}
	@Override public List<Report> subList(int fromIndex, int toIndex){return Collections.unmodifiableList(reports).subList(fromIndex, toIndex);}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override public boolean equals(Object o){
		return reports.equals(o);
	}
	@Override public int hashCode(){
		return reports.hashCode();
	}

	@Override public String toString(){
		return reports.toString();
	}

	/**
	 * Mutable variation of {@link ReportList}.
	 */
	public static class Mutable extends ReportList{
		public Mutable(){}
		public Mutable(Collection<Report> other){
			super(other);
		}

		@Override public boolean add(Report report){return reports.add(report);}
		@Override public boolean remove(Object o){return reports.remove(o);}
		@Override public boolean addAll(@NotNull Collection<? extends Report> c){return reports.addAll(c);}
		@Override public boolean addAll(int index, @NotNull Collection<? extends Report> c){return reports.addAll(index, c);}
		@Override public boolean removeAll(@NotNull Collection<?> c){return reports.removeAll(c);}
		@Override public boolean retainAll(@NotNull Collection<?> c){return reports.retainAll(c);}
		@Override public void replaceAll(UnaryOperator<Report> operator){reports.replaceAll(operator);}
		@Override public void sort(Comparator<? super Report> c){reports.sort(c);}
		@Override public Report set(int index, Report element){return reports.set(index, element);}
		@Override public void add(int index, Report element){reports.add(index, element);}
		@Override public Report remove(int index){return reports.remove(index);}
		@Override public boolean removeIf(Predicate<? super Report> filter){return reports.removeIf(filter);}

		@Override public Iterator<Report> iterator(){return reports.iterator();}
		@Override public ListIterator<Report> listIterator(){return reports.listIterator();}
		@Override public ListIterator<Report> listIterator(int index){return reports.listIterator(index);}
		@Override public List<Report> subList(int fromIndex, int toIndex){return reports.subList(fromIndex, toIndex);}
	}
}
