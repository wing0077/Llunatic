package it.unibas.lunatic.gui.node.chase.mc;

import org.openide.nodes.Node;

/**
 *
 * @author Tony
 */
public interface IChaseTreeVisitor {

    public void visitRoot(Node root);

    public void visitIntermediateNode(Node currentNode);

    public void visitLeaf(Node leaf);
}
