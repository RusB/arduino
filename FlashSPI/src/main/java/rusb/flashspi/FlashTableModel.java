/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rusb.flashspi;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author bryhljaev
 */
public class FlashTableModel extends DefaultTableModel {

    private static final int SECTORS = 16;

    public FlashTableModel() {
        init();
    }

    private void init() {
        addColumn("Block");
        for (int i = 0; i < SECTORS; ++i) {
            addColumn(String.format("#%02d", i));
        }
    }

    void setQuantityOfBlocks(int n) {
        while (getRowCount() > 0) {
            removeRow(0);
        }

        for (int i = 0; i < n; i++) {
            Object[] rowData = new Object[1 + SECTORS];
            Arrays.fill(rowData, SectorData.UNKNOWN);
            rowData[0] = String.format("#%02d", i);
            addRow(rowData);
        }
    }

    void load(Path path) {
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path), 32 * 1024)) {
            int r = 0, c = 0;
            do {
                byte[] bs = new byte[4 * 1024];
                int len = bis.read(bs);
                if (len <= 0) {
                    break;
                }

                setValueAt(new SectorData(bs, len), r, 1 + c);

                if (++c >= SECTORS) {
                    c = 0;
                    ++r;
                }
            } while (true);
        } catch (Exception ex) {
            Logger.getLogger(FlashTableModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void save(Path toPath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
