package among.exception;

/**
 * {@link Sussy} exception reserved for mismatching types.
 */
public class SussyCast extends Sussy{
	public SussyCast(Class<?> expected, Class<?> actual){
		super("Cannot cast "+actual.getName()+" to "+expected.getName());
	}
}
