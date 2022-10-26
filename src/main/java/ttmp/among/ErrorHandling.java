package ttmp.among;

/**
 * Specifies error handling mode for various invalid inputs.
 * <table summary="">
 *   <tr>
 *     <td>Value</td><td>Behavior</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ErrorHandling#ERROR}</td> <td>All invalid inputs will be reported as compilation errors.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ErrorHandling#WARN}</td> <td>All invalid inputs will be reported as warnings.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link ErrorHandling#IGNORE}</td> <td>All invalid inputs will be ignored.</td>
 *   </tr>
 * </table>
 * <br>
 * This value does not modify the way invalid inputs are being read; it will produce identical results regardless
 * of the mode used.<br>
 * If any other value is provided, the default value will be used; see the individual use cases for details.
 */
public interface ErrorHandling{
	/**
	 * All invalid inputs will be reported as compilation errors.
	 */
	int ERROR = 0;
	/**
	 * All invalid inputs will be reported as warnings.
	 */
	int WARN = 1;
	/**
	 * All invalid inputs will be ignored.
	 */
	int IGNORE = 2;
}
