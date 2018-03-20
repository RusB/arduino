/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rusb.flashspi;

import rusb.arduino.ArduinoSpi;
import gnu.io.CommPortIdentifier;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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
                showMessageError(parent, message, "Очистка ИС");
            } else {
                for (int b = 0; b < numBlocks; ++b) {
                    for (int s = 0; s < numSectors; ++s) {
                        tm.setSectorData(b, s, SectorData.UNKNOWN);
                    }
                }
                String message = "ИС очищена.";
                showMessageInfo(parent, message, "Очистка ИС");
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
                    ByteArrayOutputStream bs = new ByteArrayOutputStream(4 * 1024);

                    int addr = count * 4 * 1024;

                    for (int i = 0; i < 4; i++) {
                        if (parent.isStop()) {
                            break blocks;
                        }

                        byte[] buf = new byte[1024];

                        parent.setInfo("Считывание сектора:" + b + ":" + s + ":" + (addr + i * buf.length));
                        cp.readBlock(addr + i * buf.length, buf);
                        cp.readLine();

                        String line = cp.readLine();
                        if (!line.startsWith("OK")) {
                            List<String> lines = new ArrayList<>();
                            lines.add(line);
                            lines.add("Ошибка при считывании сектора:" + b + ":" + s + ":" + (addr + i * buf.length));
                            lines.add("Считано секторов " + count);
                            String message = String.join(System.lineSeparator(), lines);
                            showMessageError(parent, message, "Ошибка!");
                            return;
                        }

                        bs.write(buf);
                    }

                    tableModel.setSectorData(b, s, new SectorData(bs.toByteArray()));

                    ++count;
                }
            }
            LocalTime stop = LocalTime.now();

            List<String> lines = new ArrayList<>();
            lines.add(duration(start, stop));
            lines.add("Считано секторов " + count);
            String message = String.join(System.lineSeparator(), lines);
            showMessageInfo(parent, message, "Готово!");
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
                        //cp.writeLine("e" + String.valueOf(addr));
                    } else {
                        byte[] bs = sd.getBytes();
                        for (int offset = 0; offset < 1024; offset += 256) {
                            cp.writeBlock(count * bs.length + offset,
                                    Arrays.copyOfRange(bs, offset, offset + 256));

                            String line = cp.readLine();
                            if (!line.startsWith("OK")) {
                                List<String> lines = new ArrayList<>();
                                lines.add(line);
                                lines.add("Ошибка при записи сектора:" + b + ":" + s);
                                lines.add("Записано секторов " + count);
                                String message = String.join(System.lineSeparator(), lines);
                                showMessageError(parent, message, "Ошибка!");
                                return;
                            }
                        }
                    }

                    ++count;
                }
            }
            LocalTime stop = LocalTime.now();

            List<String> lines = new ArrayList<>();
            lines.add(duration(start, stop));
            lines.add("Записано секторов " + count);
            String message = String.join(System.lineSeparator(), lines);
            showMessageInfo(parent, message, "Готово!");
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
            showMessageInfo(parent, message, "Информация о МС");
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
                        showMessageError(parent, message, "Ошибка!");
                        return;
                    }

                    byte[] mbs = tableModel.getSectorData(b, s).getBytes();
                    if (!Arrays.equals(bs, mbs)) {
                        String message = "Ошибка при сравнении сектора:" + b + ":" + s;
                        showMessageError(parent, message, "Ошибка!");
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
            showMessageInfo(parent, message, "Готово!");
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String duration(LocalTime start, LocalTime stop) {
        Duration d = Duration.between(start, stop);
        return d.toString();
    }

    private void showMessageError(MainJFrame parent, String message, String title) {
        showMessage(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showMessageInfo(MainJFrame parent, String message, String title) {
        showMessage(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMessage(MainJFrame parent, String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, title, messageType);
        });
    }

}
