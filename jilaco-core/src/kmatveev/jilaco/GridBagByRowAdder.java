package kmatveev.jilaco;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

public class GridBagByRowAdder {

    private final List<GridBagConstraints> columnConfig;
    private int row = 0;

    public GridBagByRowAdder(GridBagConstraints... columnConfig) {
        List<GridBagConstraints> configs = Arrays.asList(columnConfig);
        for (int i = 0; i < configs.size(); i++) {
            configs.get(i).gridx = i;
        }
        this.columnConfig = configs;
    }

    public void addRow(JComponent parent, JComponent... components) {
        if (components.length != columnConfig.size()) {
            throw new IllegalArgumentException("Row size doesn't match");
        }

        for (int i = 0; i < components.length; i++) {
            GridBagConstraints gbc = columnConfig.get(i);
            gbc.gridy = row;
            parent.add(components[i], gbc);
        }

        row++;
    }

    public void addSingleComponentWholeRow(JComponent parent, JComponent component, Insets insets) {

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = columnConfig.size();
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        gbc.insets = insets;

        parent.add(component, gbc);

        row++;
    }


    public void addBottomFillerTo(JComponent parent) {
        for (int i = 0; i < columnConfig.size(); i++) {
            GridBagConstraints gbc = (GridBagConstraints)columnConfig.get(i).clone();
            gbc.gridy = row;
            gbc.weighty = 1.0;
            JComponent filler = new JPanel();
            filler.setOpaque(false);
            parent.add(filler, gbc);
        }

    }

}
