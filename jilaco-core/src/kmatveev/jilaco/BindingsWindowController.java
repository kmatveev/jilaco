package kmatveev.jilaco;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class BindingsWindowController {

    private final MainWindowController mainWindowController;
    private final MainWindowController.Evaluator evaluator;

    public BindingsWindowController(MainWindowController mainWindowController, MainWindowController.Evaluator evaluator) {
        this.mainWindowController = mainWindowController;
        this.evaluator = evaluator;
    }

    public void showBindingsWindow() {
        JFrame bindingsWindow = new JFrame("Bindings");

        DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Invisible root"));
        Map<String, Object> bindings = evaluator.getBindings();
        if (bindings != null) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            populateRecursively(bindings, root);
        }

        JTree tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        // tree.setEnabled(false);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    mainWindowController.evalAndPrint(((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).getUserObject().toString(), evaluator);
                }

            }
        });

        bindingsWindow.getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);
        bindingsWindow.setSize(500, 400);
        bindingsWindow.show();

    }

    private static void populateRecursively(Map<String, Object> bindings, DefaultMutableTreeNode parent) {
        for (Map.Entry<String, Object> binding : bindings.entrySet()) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(binding.getKey());
            parent.add(child);
            if (binding.getValue() instanceof Map) {
                populateRecursively((Map<String, Object>)binding.getValue(), child);
            }
        }
    }
}
