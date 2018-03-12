/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rusb.flashspi;

import rusb.arduino.ArduinoSpi;
import gnu.io.CommPortIdentifier;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import rusb.arduino.ArduinoCommPort;

/**
 *
 * @author ruslan
 */
public class ArduinoSpiEeprom extends ArduinoSpi {

    public static final int numSectors = 16;

    private int numBlocks = 64;

    ArduinoSpiEeprom(CommPortIdentifier cpi) {
        super(cpi);
    }

    void flashErase(MainJFrame parent, FlashTableModel tm) {
        try (ArduinoCommPort cp = openCommPort()) {
            cp.writeLine("a");

            String line = cp.readLine();
            if (!line.startsWith("OK")) {
                List<String> lines = new ArrayList<>();
                lines.add(line);
                lines.add("Ошибка при очистке ИС.");
                String message = String.join(System.lineSeparator(), lines);
                JOptionPane.showMessageDialog(parent, message, "Очистка ИС", JOptionPane.ERROR_MESSAGE);
            } else {
                for (int b = 0; b < numBlocks; ++b) {
                    for (int s = 0; s < numSectors; ++s) {
                        tm.setSectorData(b, s, SectorData.UNKNOWN);
                    }
                }
                String message = "ИС очищена.";
                JOptionPane.showMessageDialog(parent, message, "Очистка ИС", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    void flashRead(MainJFrame parent, FlashTableModel tableModel) {
        try (ArduinoCommPort cp = openCommPort()) {
            int count = 0;

            LocalTime start = LocalTime.now();
            blocks:
            for (int b = 0; b < numBlocks; ++b) {
                for (int s = 0; s < numSectors; ++s) {
                    parent.setInfo("Считывание сектора:" + b + ":" + s);
                    if (parent.isStop()) {
                        break blocks;
                    }

                    byte[] bs = new byte[4 * 1024];

                    cp.readBlock(count * bs.length, bs);
                    cp.readLine();

                    String line = cp.readLine();
                    if (!line.startsWith("OK")) {
                        List<String> lines = new ArrayList<>();
                        lines.add(line);
                        lines.add("Ошибка при считывании сектора:" + b + ":" + s);
                        lines.add("Считано секторов " + count);
                        String message = String.join(System.lineSeparator(), lines);
                        JOptionPane.showMessageDialog(parent, message, "Ошибка!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    tableModel.setSectorData(b, s, new SectorData(bs));

                    ++count;
                }
            }
            LocalTime stop = LocalTime.now();

            List<String> lines = new ArrayList<>();
            lines.add(duration(start, stop));
            lines.add("Считано секторов " + count);
            String message = String.join(System.lineSeparator(), lines);
            JOptionPane.showMessageDialog(parent, message, "Готово!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    void flashWrite(MainJFrame parent, FlashTableModel tableModel) {
        try (ArduinoCommPort cp = openCommPort()) {
            int count = 0;

            LocalTime start = LocalTime.now();
            blocks:
            for (int b = 0; b < numBlocks; ++b) {
                for (int s = 0; s < numSectors; ++s) {
                    parent.setInfo("Запись сектора:" + b + ":" + s);
                    if (parent.isStop()) {
                        break blocks;
                    }

                    SectorData sd = tableModel.getSectorData(b, s);

                    if (sd.isEmpty()) {
                        int addr = count * 4 * 1024;
                        cp.writeLine("e" + String.valueOf(addr));
                    } else {
                        byte[] bs = sd.getBytes();
                        cp.writeBlock(count * bs.length, bs);
                    }

                    String line = cp.readLine();
                    if (!line.startsWith("OK")) {
                        List<String> lines = new ArrayList<>();
                        lines.add(line);
                        lines.add("Ошибка при записи сектора:" + b + ":" + s);
                        lines.add("Записано секторов " + count);
                        String message = String.join(System.lineSeparator(), lines);
                        JOptionPane.showMessageDialog(parent, message, "Ошибка!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ++count;
                }
            }
            LocalTime stop = LocalTime.now();

            List<String> lines = new ArrayList<>();
            lines.add(duration(start, stop));
            lines.add("Записано секторов " + count);
            String message = String.join(System.lineSeparator(), lines);
            JOptionPane.showMessageDialog(parent, message, "Готово!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    void flashInfo(MainJFrame parent) {
        try (ArduinoCommPort cp = openCommPort()) {
            cp.writeLine("i");

            List<String> lines = new ArrayList<>();

            do {
                String line = cp.readLine();
                if (line.startsWith("OK")) {
                    break;
                }
                lines.add(line);
            } while (true);

            String message = String.join(System.lineSeparator(), lines);
            JOptionPane.showMessageDialog(parent, message, "Информация о МС", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    void flashVerify(MainJFrame parent, FlashTableModel tableModel) {
        try (ArduinoCommPort cp = openCommPort()) {
            int count = 0;

            LocalTime start = LocalTime.now();
            blocks:
            for (int b = 0; b < numBlocks; ++b) {
                for (int s = 0; s < numSectors; ++s) {
                    parent.setInfo("Считывание сектора:" + b + ":" + s);
                    if (parent.isStop()) {
                        break blocks;
                    }

                    byte[] bs = new byte[4 * 1024];

                    cp.readBlock(count * bs.length, bs);
                    cp.readLine();

                    String line = cp.readLine();
                    if (!line.startsWith("OK")) {
                        List<String> lines = new ArrayList<>();
                        lines.add(line);
                        lines.add("Ошибка при считывании сектора:" + b + ":" + s);
                        lines.add("Считано секторов " + count);
                        String message = String.join(System.lineSeparator(), lines);
                        JOptionPane.showMessageDialog(parent, message, "Ошибка!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    byte[] mbs = tableModel.getSectorData(b, s).getBytes();
                    if (!Arrays.equals(bs, mbs)) {
                        String message = "Ошибка при сравнении сектора:" + b + ":" + s;
                        JOptionPane.showMessageDialog(parent, message, "Ошибка!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ++count;
                }
            }
            LocalTime stop = LocalTime.now();

            List<String> lines = new ArrayList<>();
            lines.add(duration(start, stop));
            lines.add("Считано секторов " + count);
            String message = String.join(System.lineSeparator(), lines);
            JOptionPane.showMessageDialog(parent, message, "Готово!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String duration(LocalTime start, LocalTime stop) {
        Duration d = Duration.between(start, stop);
        return d.toString();
    }

}
