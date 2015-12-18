package beast.app.beauti;

import javax.swing.*;
import javax.swing.table.*;

import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;
import beast.util.NexusParser;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class AlignmentViewer extends JPanel {
    private static final long serialVersionUID = 1L;

    Object[][] tableData;
    Object[] columnData;
    boolean useColor = false;
    // flag to indicate that the most frequently occurring character is shown as a dot
    boolean useDots = true;
    Alignment m_alignment;
    Map<Character, Color> m_customColorMap = new HashMap<>();

    /**
     * define which character maps to which color *
     */
    public void setCustomColorMap(Map<Character, Color> colorMap) {
        for (char c : m_customColorMap.keySet()) {
            m_customColorMap.put(c, colorMap.get(c));
        }
    }

    /**
     * constructor processes alignment and sets up table with first column fixed *
     */
    public AlignmentViewer(Alignment data) throws Exception {
        m_alignment = data;
        int nSites = data.getSiteCount();
        int nTaxa = data.getNrTaxa();
        tableData = new Object[nTaxa][nSites + 1];
        char[] headerChar = updateTableData();

        // set up row labels
        for (int i = 0; i < nTaxa; i++) {
            tableData[i][0] = data.getTaxaNames().get(i);
        }

        // set up column labels
        columnData = new Object[nSites + 1];
        for (int i = 1; i < nSites + 1; i++) {
            columnData[i] = "<html>.<br>" + headerChar[i - 1] + "</html>";
        }
        columnData[0] = "<html><br>taxon name</html>";
        columnData[1] = "<html>1<br>" + headerChar[0] + "</html>";
        for (int i = 10; i < nSites; i += 10) {
            String s = i + "";
            for (int j = 0; j < s.length(); j++) {
            	if (i+j < columnData.length) {
            		columnData[i + j] = "<html>" + s.charAt(j) + "<br>" + headerChar[i - 1] + "</html>";
            	}
            }
            columnData[i - 5] = "<html>+<br>" + headerChar[i - 1] + "</html>";
        }

        // create table in scrollpane with first column fixed
        final TableModel fixedColumnModel = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            public int getColumnCount() {
                return 1;
            }

            public String getColumnName(int column) {
                return columnData[column] + "";
            }

            public int getRowCount() {
                return tableData.length;
            }

            public Object getValueAt(int row, int column) {
                return tableData[row][column];
            }
        };

        final TableModel mainModel = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            public int getColumnCount() {
                return columnData.length - 1;
            }

            public String getColumnName(int column) {
                return columnData[column + 1] + "";
            }

            public int getRowCount() {
                return tableData.length;
            }

            public Object getValueAt(int row, int column) {
                return tableData[row][column + 1];
            }
        };

        JTable fixedTable = new JTable(fixedColumnModel);
        fixedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Font font = fixedTable.getFont();
        font = new Font(font.getFontName(), font.getStyle(), 8);
        fixedTable.setFont(font);
        TableColumn col = fixedTable.getColumnModel().getColumn(0);
        col.setPreferredWidth(200);
        fixedTable.getTableHeader().setFont(font);

        JTable mainTable = new JTable(mainModel);
        mainTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        mainTable.setFont(font);
        mainTable.getTableHeader().setFont(font);
        for (int i = 0; i < nSites; i++) {
            col = mainTable.getColumnModel().getColumn(i);
            col.setPreferredWidth(6);
        }

        ListSelectionModel model = fixedTable.getSelectionModel();
        mainTable.setSelectionModel(model);
        mainTable.setShowGrid(false);

        JScrollPane scrollPane = new JScrollPane(mainTable);
        Dimension fixedSize = fixedTable.getPreferredSize();
        JViewport viewport = new JViewport();
        viewport.setView(fixedTable);
        viewport.setPreferredSize(fixedSize);
        viewport.setMaximumSize(fixedSize);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedTable.getTableHeader());
        scrollPane.setRowHeaderView(viewport);


        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    private char[] updateTableData() {
        int nSites = m_alignment.getSiteCount();
        int nTaxa = m_alignment.getNrTaxa();

        // set up table content
        DataType dataType = m_alignment.getDataType();
        char[] headerChar = new char[nSites];
        Object[][] colorMap = setupColorMap();
        try {
            for (int i = 0; i < nSites; i++) {
                int iPattern = m_alignment.getPatternIndex(i);
                int[] pattern = m_alignment.getPattern(iPattern);
                String sPattern = dataType.state2string(pattern);
                headerChar[i] = mostFrequentCharInPattern(sPattern);
                for (int j = 0; j < nTaxa; j++) {
                    char c = sPattern.charAt(j);
                    if (c == headerChar[i]) {
                        tableData[j][i + 1] = colorMap[0][c];
                    } else {
                        tableData[j][i + 1] = colorMap[1][c];
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return headerChar;
    }

    /**
     * determine content of table cells.
     * Without color, only Characters are displayed, which can be a bit faster than using color
     * With color, the color is encoded in HTML
     *
     * @return an array of 2x256 where the first entry is for the most frequently occurring character,
     *         and the second for the others
     *         *
     */
    private Object[][] setupColorMap() {
        if (useColor) {
            String[][] colorMap = new String[2][256];
            for (int k = 'A'; k < 'Z'; k++) {
                int i = k - 'A';
                int nRed = ((i & 0x80) >> 7) + ((i & 0x10) >> 4) + ((i & 0x2) << 1);
                int nGreen = ((i & 0x40) >> 6) + ((i & 0x08) >> 2) + ((i & 0x4));
                int nBlue = ((i & 0x20) >> 5) + ((i & 0x04) >> 1) + ((i & 0x1) << 2);
                int nColor = (nRed << 21 + (nGreen << 18)) + (nGreen << 13) + (nBlue << 10) + (nBlue << 5) + (nRed << 2);
                colorMap[0][k] = "<html><font color='#" + Integer.toString(nColor, 16) + "'><b>.</b></html>";
                colorMap[1][k] = "<html><font color='#" + Integer.toString(nColor, 16) + "'><b>" + ((char) k) + "</font></html>";
            }
            for (char c : m_customColorMap.keySet()) {
                Color color = m_customColorMap.get(c);
                colorMap[0][c] = "<html><font color='#" + Integer.toString(color.getRGB(), 16) + "'><b>.</b></html>";
                colorMap[1][c] = "<html><font color='#" + Integer.toString(color.getRGB(), 16) + "'><b>" + c + "</font></html>";
            }
            if (!this.useDots) {
                colorMap[0] = colorMap[1];
            }
            return colorMap;
        } else {
            Character[][] colorMap = new Character[2][256];
            for (int i = 0; i < 256; i++) {
                colorMap[0][i] = '.';
                colorMap[1][i] = (char) i;
            }
            if (!this.useDots) {
                colorMap[0] = colorMap[1];
            }
            return colorMap;
        }
    }

    private char mostFrequentCharInPattern(String sPattern) {
        char[] counts = new char[256];
        for (int i = 0; i < sPattern.length(); i++) {
            counts[sPattern.charAt(i)]++;
        }
        int iMax = 0, nMax = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > nMax) {
                iMax = i;
                nMax = counts[i];
            }
        }
        return (char) iMax;
    }

    public void showInDialog() {
        JDialog dlg = new JDialog();
        dlg.setName("AlignmentViewer");
        dlg.add(this);

        Box buttonBox = Box.createHorizontalBox();
        JCheckBox useDotsCheckBox = new JCheckBox("Use dots", true);
        useDotsCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox _useDots = (JCheckBox) e.getSource();
                useDots = _useDots.isSelected();
                updateTableData();
                repaint();
            }
        });
        buttonBox.add(useDotsCheckBox);

        JCheckBox useColorCheckBox = new JCheckBox("Use Color");
        useColorCheckBox.setName("UseColor");
        useColorCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox hasColor = (JCheckBox) e.getSource();
                useColor = hasColor.isSelected();
                updateTableData();
                repaint();
            }
        });
        buttonBox.add(useColorCheckBox);
        dlg.add(buttonBox, BorderLayout.SOUTH);

        dlg.setSize(1024, 600);
        dlg.setModal(true);
        dlg.setVisible(true);
        dlg.dispose();
    }

    public static void main(String[] args) {
        try {
            NexusParser parser = new NexusParser();
            parser.parseFile(new File(args[0]));
            Alignment data = parser.m_alignment;
            AlignmentViewer panel = new AlignmentViewer(data);
            panel.showInDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
