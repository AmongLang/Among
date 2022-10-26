package ttmp.among;

import java.util.ArrayList;
import java.util.List;

public final class NodePathBuilder{
	private final List<NodePath.Element> pathElements = new ArrayList<>();

	public NodePathBuilder index(int idx){
		pathElements.add(new NodePath.Index(idx));
		return this;
	}

	public NodePathBuilder prop(String prop){
		pathElements.add(new NodePath.Property(prop));
		return this;
	}

	public NodePath of(){
		return NodePath.of(pathElements);
	}
}
