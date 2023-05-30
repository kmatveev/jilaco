package kmatveev.jilaco;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Heavily modified code from
// https://github.com/aterai/java-swing-tips/tree/master/DnDLayerTabbedPane
public class CloseButtonTabComponentFactory {

    public static Component createCloseButtonTabComponent(JTabbedPane tabbedPane) {

        ActionListener defaultActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component buttonTabComponent = (Component)e.getSource();
                int i = tabbedPane.indexOfTabComponent(buttonTabComponent);
                if (i != -1) {
                    tabbedPane.remove(i);
                }
            }
        };

        return createCloseButtonTabComponent(tabbedPane, defaultActionListener);

    }

    public static Component createCloseButtonTabComponent(JTabbedPane tabbedPane, ActionListener actionListener) {
        JPanel buttonTabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonTabComponent.setOpaque(false);
        JLabel label = new JLabel() {
            @Override public String getText() {
                int i = tabbedPane.indexOfTabComponent(buttonTabComponent);
                if (i != -1) {
                    return tabbedPane.getTitleAt(i);
                }
                return null;
            }

            @Override public Icon getIcon() {
                int i = tabbedPane.indexOfTabComponent(buttonTabComponent);
                if (i != -1) {
                    return tabbedPane.getIconAt(i);
                }
                return null;
            }
        };
        buttonTabComponent.add(label);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        JButton button = new CloseButton();

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // we change event source from close button to tab component
                actionListener.actionPerformed(new ActionEvent(buttonTabComponent, e.getID(), e.getActionCommand(), e.getModifiers()));
            }
        });
        button.addMouseListener(new MouseAdapter () {

            @Override public void mouseEntered(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(true);
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(false);
                }
            }
        });
        buttonTabComponent.add(button);

        buttonTabComponent.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        return buttonTabComponent;

    }

    private static class CloseButton extends JButton {
        private static final int SZ = 17;
        private static final int DELTA = 6;

        protected CloseButton() {
            super();
            setUI(new BasicButtonUI());
            setToolTipText("close this tab");
            setContentAreaFilled(false);
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            setRolloverEnabled(true);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(SZ, SZ);
        }

        @Override public void updateUI() {
            // we don't want to update UI for this button
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setStroke(new BasicStroke(2));
            g2.setPaint(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setPaint(Color.ORANGE);
            }
            if (getModel().isPressed()) {
                g2.setPaint(Color.BLUE);
            }
            g2.drawLine(DELTA, DELTA, getWidth() - DELTA - 1, getHeight() - DELTA - 1);
            g2.drawLine(getWidth() - DELTA - 1, DELTA, DELTA, getHeight() - DELTA - 1);
            g2.dispose();
        }
    }

}
