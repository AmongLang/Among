package among;

import among.obj.Among;
import among.obj.AmongList;
import among.obj.AmongObject;
import among.obj.AmongPrimitive;

/**
 * Visitor for {@link Among} objects.
 *
 * @see Among#walk(AmongWalker)
 * @see Among#walk(AmongWalker, NodePath)
 */
@FunctionalInterface
public interface AmongWalker{
	/**
	 * Performs operation on the visited object. Modifying the instance is permitted.
	 *
	 * @param primitive The object being visited
	 * @param path      Relative path of the object, from starting point of the walk
	 */
	void walk(AmongPrimitive primitive, NodePath path);
	/**
	 * Performs operation on the visited object. Modifying the instance is permitted.
	 *
	 * @param object The object being visited
	 * @param path   Relative path of the object, from starting point of the walk
	 * @return Whether it will walk down the object; returning {@code false} will prevent walker from visiting elements inside
	 * {@code object}.
	 */
	default boolean walkBefore(AmongObject object, NodePath path){
		return true;
	}
	/**
	 * Performs operation on the visited object. Modifying the instance is permitted.
	 *
	 * @param list The object being visited
	 * @param path Relative path of the object, from starting point of the walk
	 * @return Whether it will walk down the list; returning {@code false} will prevent walker from visiting elements inside
	 * {@code list}.
	 */
	default boolean walkBefore(AmongList list, NodePath path){
		return true;
	}

	default void walkAfter(AmongObject object, NodePath path){}
	default void walkAfter(AmongList list, NodePath path){}
}
