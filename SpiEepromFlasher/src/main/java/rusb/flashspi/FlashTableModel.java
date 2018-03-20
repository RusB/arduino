/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rusb.flashspi;

import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author bryhljaev
 */
public class FlashTableModel extends DefaultTableModel {

    public FlashTableModel() {
        init();
    }

    private void init() {
        addColumn("Sector");
        for (int i = 0; i < ArduinoSpiEeprom.numSectors; ++i) {
            addColumn(String.format("#%02d", i));
        }
    }

    void setQuantityOfBlocks(int n) {
        while (getRowCount() > 0) {
            removeRow(0);
        }

        for (int i = 0; i < n; i++) {
            Object[] rowData = new Object[1 + ArduinoSpiEeprom.numSectors];
            Arrays.fill(rowData, SectorData.UNKNOWN);
            rowData[0] = String.format("Block #%02d", i);
            addRow(rowData);
        }
    }

    public SectorData getSectorData(int block, int sector) {
        Object obj = getValueAt(block, 1 + sector);
        return (SectorData) obj;
    }

    public void setSectorData(int block, int sector, SectorData data) {
        SwingUtilities.invokeLater(() -> setValueAt(data, block, 1 + sector));
    }

}
