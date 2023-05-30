package kmatveev.jilaco;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import javax.swing.plaf.metal.MetalTabbedPaneUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.TooManyListenersException;

// Heavily modified code from
// https://github.com/aterai/java-swing-tips/tree/master/DnDLayerTabbedPane
public class DraggableTabbedPaneFactory {

    // creates tabbed pane which supports drag within itself only
    public static JLayer<? extends JTabbedPane> createDraggableTabbedPane() {

        DraggableTabbedPane tabbedPane = new DraggableTabbedPane();
        LayerUI<DraggableTabbedPane> layerUI = new DropLocationLayerUI();

        TabTransferHandler tabTransferHandler = new TabTransferHandler();
        tabbedPane.setTransferHandler(tabTransferHandler);
        try {
            tabbedPane.getDropTarget().addDropTargetListener(new TabDropTargetAdapter());
        } catch (TooManyListenersException ex) {
            ex.printStackTrace();
            UIManager.getLookAndFeel().provideErrorFeedback(tabbedPane);
        }

        MouseHandler mouseHandler = new MouseHandler();
        tabbedPane.addMouseListener(mouseHandler);
        tabbedPane.addMouseMotionListener(mouseHandler);


        return new JLayer<>(tabbedPane, layerUI);
    }

    private static class DraggableTabbedPane extends JTabbedPane {
        public int dragTabIndex = -1;
        private int dropTabIndex = -1;

        public void setDropTabIndex(int newDropTabIndex) {
            if (dropTabIndex != newDropTabIndex) {
                int old = dropTabIndex;
                dropTabIndex = newDropTabIndex;

                // instead of listening for special property event, we will repaint directly
                repaint();  // firePropertyChange("dropTabIndex", old, dropTabIndex);
            }
        }

        public void convertTab(int prev, int next) {
            final Component cmp = getComponentAt(prev);
            final Component tab = getTabComponentAt(prev);
            final String title = getTitleAt(prev);
            final Icon icon = getIconAt(prev);
            final String tip = getToolTipTextAt(prev);
            final boolean isEnabled = isEnabledAt(prev);
            int tgtIndex = prev > next ? next : next - 1;
            remove(prev);
            insertTab(title, icon, cmp, tip, tgtIndex);
            setEnabledAt(tgtIndex, isEnabled);
            if (isEnabled) {
                setSelectedIndex(tgtIndex);
            }
            setTabComponentAt(tgtIndex, tab);
        }

        private BufferedImage makeDragTabImage() {
            JTabbedPane tabs = this;
            Rectangle rect = tabs.getBoundsAt(dragTabIndex);
            int w = tabs.getWidth();
            int h = tabs.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.createGraphics();
            tabs.paint(g);
            g.dispose();
            if (rect.x < 0) {
                rect.translate(-rect.x, 0);
            }
            if (rect.y < 0) {
                rect.translate(0, -rect.y);
            }
            if (rect.x + rect.width > img.getWidth()) {
                rect.width = img.getWidth() - rect.x;
            }
            if (rect.y + rect.height > img.getHeight()) {
                rect.height = img.getHeight() - rect.y;
            }
            return img.getSubimage(rect.x, rect.y, rect.width, rect.height);
        }


    }

    public static Rectangle getTabAreaBounds(JTabbedPane tabbedPane) {
        Rectangle tabbedRect = tabbedPane.getBounds();
        int xx = tabbedRect.x;
        int yy = tabbedRect.y;
        Rectangle compRect = Optional.ofNullable(tabbedPane.getSelectedComponent())
                .map(Component::getBounds)
                .orElseGet(Rectangle::new);
        int tabPlacement = tabbedPane.getTabPlacement();
        if (isTopBottomTabPlacement(tabPlacement)) {
            tabbedRect.height = tabbedRect.height - compRect.height;
            if (tabPlacement == SwingConstants.BOTTOM) {
                tabbedRect.y += compRect.y + compRect.height;
            }
        } else {
            tabbedRect.width = tabbedRect.width - compRect.width;
            if (tabPlacement == SwingConstants.RIGHT) {
                tabbedRect.x += compRect.x + compRect.width;
            }
        }
        tabbedRect.translate(-xx, -yy);
        return tabbedRect;
    }

    public static boolean isTopBottomTabPlacement(int tabPlacement) {
        return tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM;
    }
    public static int tabDropLocationForPoint(JTabbedPane tabbedPane, Point p) {

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getBoundsAt(i).contains(p)) {
                return i;
            }
        }
        if (getTabAreaBounds(tabbedPane).contains(p)) {
            return tabbedPane.getTabCount();
        }
        return -1;
    }

    private static class MouseHandler extends MouseAdapter  {
        private Point startPt;
        private final int dragThreshold = DragSource.getDragThreshold();

        @Override
        public void mousePressed(MouseEvent e) {
            DraggableTabbedPane src = (DraggableTabbedPane) e.getComponent();
            boolean isOnlyOneTab = src.getTabCount() <= 1;
            if (isOnlyOneTab) {
                startPt = null;
                return;
            }
            Point tabPt = e.getPoint(); // e.getDragOrigin();
            int idx = src.indexAtLocation(tabPt.x, tabPt.y);
            boolean flag = idx < 0 || !src.isEnabledAt(idx) || Objects.isNull(src.getComponentAt(idx));
            startPt = flag ? null : tabPt;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point tabPt = e.getPoint(); // e.getDragOrigin();
            if (Objects.nonNull(startPt) && startPt.distance(tabPt) > dragThreshold) {
                DraggableTabbedPane src = (DraggableTabbedPane) e.getComponent();
                TransferHandler th = src.getTransferHandler();
                int idx = src.indexAtLocation(tabPt.x, tabPt.y);
                int selIdx = src.getSelectedIndex();
                boolean isRotateTabRuns = !(src.getUI() instanceof MetalTabbedPaneUI)
                        && src.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT
                        && idx != selIdx;
                src.dragTabIndex = isRotateTabRuns ? selIdx : idx;
                th.exportAsDrag(src, e, TransferHandler.MOVE);
                startPt = null;
            }
        }
    }



    private static class TabDropTargetAdapter extends DropTargetAdapter {

        private void clearDropLocationPaint(Component c) {
            if (c instanceof DraggableTabbedPane) {
                DraggableTabbedPane t = (DraggableTabbedPane) c;
                t.setDropTabIndex(-1);
                t.setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            Component c = e.getDropTargetContext().getComponent();
            clearDropLocationPaint(c);
        }

        @Override
        public void dragExit(DropTargetEvent e) {
            Component c = e.getDropTargetContext().getComponent();
            clearDropLocationPaint(c);
        }

    }

    enum DragImageMode {
        HEAVYWEIGHT, LIGHTWEIGHT
    }

    private static class TabTransferHandler extends TransferHandler {
        protected final DataFlavor localObjectFlavor = new DataFlavor(DraggableTabbedPane.class, "DraggableTabbedPane") {
            public boolean isRepresentationClassSerializable() {
                return false;
            }
        };

        protected final JLabel label = new JLabel() {
            @Override
            public boolean contains(int x, int y) {
                return false;
            }
        };
        protected final JWindow dialog = new JWindow();
        protected DragImageMode mode = DragImageMode.LIGHTWEIGHT;

        public void setDragImageMode(DragImageMode dragMode) {
            this.mode = dragMode;
            setDragImage(null);
        }

        protected TabTransferHandler() {
            super();

            dialog.add(label);
            dialog.setOpacity(.5f);
            DragSource.getDefaultDragSource().addDragSourceMotionListener(e -> {
                Point pt = e.getLocation();
                pt.translate(5, 5); // offset
                dialog.setLocation(pt);
            });
        }

        @Override
        protected Transferable createTransferable(JComponent c) {

            if (c instanceof DraggableTabbedPane) {
                DraggableTabbedPane source = (DraggableTabbedPane) c;

                if (mode == DragImageMode.HEAVYWEIGHT) {
                    label.setIcon(new ImageIcon(source.makeDragTabImage()));
                    dialog.pack();
                    dialog.setVisible(true);
                } else {
                    setDragImage(source.makeDragTabImage());
                }

                return new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{localObjectFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return Objects.equals(localObjectFlavor, flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                        if (isDataFlavorSupported(flavor)) {
                            return source;
                        } else {
                            throw new UnsupportedFlavorException(flavor);
                        }
                    }
                };
            } else {
                return null;
            }
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(localObjectFlavor)) {
                return false;
            }

            JTabbedPane src;
            try {
                // since we've checked that data flawour is supported, we can request this kind of data
                src = (JTabbedPane) support.getTransferable().getTransferData(localObjectFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                throw new RuntimeException(e);
            }

            support.setDropAction(TransferHandler.MOVE);
            TransferHandler.DropLocation tdl = support.getDropLocation();
            Point pt = tdl.getDropPoint();
            DraggableTabbedPane target = (DraggableTabbedPane) support.getComponent();
            int idx = tabDropLocationForPoint(target, pt);

            boolean canDrop;
            boolean inArea = getTabAreaBounds(target).contains(pt) && idx >= 0;
            if (target == src) {
                canDrop = inArea && idx != target.dragTabIndex && idx != target.dragTabIndex + 1;
            } else {
                canDrop = false;
            }

            target.setCursor(canDrop ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);

            support.setShowDropLocation(canDrop);
            target.setDropTabIndex(canDrop ? idx : -1);
            return canDrop;
        }

        @Override public boolean importData(TransferHandler.TransferSupport support) {
            DraggableTabbedPane target = (DraggableTabbedPane) support.getComponent();
            DropLocation dl = support.getDropLocation();
            try {
                JTabbedPane src = (JTabbedPane) support.getTransferable().getTransferData(localObjectFlavor);
                int index = tabDropLocationForPoint(target, dl.getDropPoint());
                if (target == src) {
                    target.convertTab(target.dragTabIndex, index);
                    return true;
                } else if (src instanceof DraggableTabbedPane){
                    // not supported yet
                    return false;
                } else {
                    return false;
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                return false;
            }
        }

        @Override public int getSourceActions(JComponent c) {
            if (c instanceof DraggableTabbedPane) {
                DraggableTabbedPane src = (DraggableTabbedPane) c;
                if (src.dragTabIndex < 0) {
                    return TransferHandler.NONE;
                }
                return TransferHandler.MOVE;
            }
            return TransferHandler.NONE;
        }

        @Override protected void exportDone(JComponent c, Transferable data, int action) {
            DraggableTabbedPane src = (DraggableTabbedPane) c;
            src.setDropTabIndex(-1);
            if (mode == DragImageMode.HEAVYWEIGHT) {
                dialog.setVisible(false);
            }
        }
    }

    private static class DropLocationLayerUI extends LayerUI<DraggableTabbedPane> {
        private static final int LINE_SZ = 3;

        @Override public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (c instanceof JLayer) {
                JLayer<?> layer = (JLayer<?>) c;
                DraggableTabbedPane tabbedPane = (DraggableTabbedPane) layer.getView();
                if (tabbedPane.dropTabIndex >= 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
                    g2.setPaint(Color.RED);

                    int a = Math.min(tabbedPane.dropTabIndex, 1); // index == 0 ? 0 : 1;
                    Rectangle r = tabbedPane.getBoundsAt(a * (tabbedPane.dropTabIndex - 1));
                    Rectangle lineRect = new Rectangle();
                    if (isTopBottomTabPlacement(tabbedPane.getTabPlacement())) {
                        lineRect.setBounds(r.x - LINE_SZ / 2 + r.width * a, r.y, LINE_SZ, r.height);
                    } else {
                        lineRect.setBounds(r.x, r.y - LINE_SZ / 2 + r.height * a, r.width, LINE_SZ);
                    }
                    g2.fill(lineRect);

                    g2.dispose();

                }
            }
        }

    }



}
