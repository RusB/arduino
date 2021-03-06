package rusb.flashspi;

import gnu.io.CommPortIdentifier;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
public class ArduinoSpiEeprom {

    public static final int numSectors = 16;

    private static final int numBlocks = 64;

    private final ArduinoCommPort commPort;

    public ArduinoSpiEeprom(CommPortIdentifier commPortIdentifier) {
        this.commPort = new ArduinoCommPort(commPortIdentifier);
    }

    protected ArduinoCommPort openCommPort() throws IOException {
        commPort.open();
        do {
            String line = commPort.readLine();
            if (line.startsWith("Init")) {
                break;
            }
        } while (true);
//        System.out.print("Установлено соединение ");
//        System.out.print(commPort.getName());
//        System.out.println();
        return commPort;
    }

    void flashEraseAll(MainJFrame parent, FlashTableModel tm) {
        try (ArduinoCommPort cp = openCommPort()) {
            parent.setInfo("Полная очистка ИС...");
            cp.writeLine("a");

            String line = cp.readLine();
            if (line.startsWith("FAIL")) {
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
                parent.setInfo("");
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    void flashEraseSector(MainJFrame parent, FlashTableModel tm, int block, int sector) {
        try (ArduinoCommPort cp = openCommPort()) {
            parent.setInfo("Очистка сектора...");

            int addr = (block * numSectors + sector) * 4 * 1024;
            cp.writeLine("e" + addr);

            String line = cp.readLine();
            if (line.startsWith("FAIL")) {
                List<String> lines = new ArrayList<>();
                lines.add(line);
                lines.add("Ошибка при очистке сектора.");
                String message = String.join(System.lineSeparator(), lines);
                showMessageError(parent, message, "Очистка сектора");
            } else {
                tm.setSectorData(block, sector, SectorData.UNKNOWN);
                String message = "Сектор очищен.";
                showMessageInfo(parent, message, "Очистка сектора");
                parent.setInfo("");
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

                    int addr = (b * numSectors + s) * 4 * 1024;

                    for (int i = 0; i < 4; i++) {
                        if (parent.isStop()) {
                            break blocks;
                        }

                        byte[] buf = new byte[1024];

                        parent.setInfo("Считывание сектора:" + b + ":" + s + ":" + (addr + i * buf.length));
                        cp.writeLine("r" + String.valueOf(addr + i * buf.length));
                        cp.readBlock(buf);

                        String line = cp.readLine();
                        if (line.startsWith("FAIL")) {
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

    void flashWrite(MainJFrame parent, FlashTableModel tableModel, int block, int sector) {
        try (ArduinoCommPort cp = openCommPort()) {
            int count = 0;

            int sb = block, ss = sector;

            LocalTime start = LocalTime.now();
            blocks:
            for (int b = sb; b < numBlocks; ++b) {
                for (int s = ss; s < numSectors; ++s) {
                    SectorData sd = tableModel.getSectorData(b, s);

                    int addr = (numSectors * b + s) * 4 * 1024;

                    if (sd.isEmpty()) {
                        cp.writeLine("e" + String.valueOf(addr));
                    } else {
                        for (int n = 0; n < 4; ++n) {
                            int from = n * 1024;

                            parent.setInfo("Запись сектора:" + b + ":" + s + ":" + (addr + from));
                            if (parent.isStop()) {
                                break blocks;
                            }

                            byte[] bs = Arrays.copyOfRange(sd.getBytes(), from, from + 1024);
                            cp.writeLine("w" + String.valueOf(addr + from));
                            cp.writeBlock(bs);

                            String line = cp.readLine();
                            if (line.startsWith("FAIL")) {
                                List<String> lines = new ArrayList<>();
                                lines.add(line);
                                lines.add("Ошибка при записи сектора:" + b + ":" + s + ":" + (addr + from));
                                lines.add("Записано секторов " + count);
                                String message = String.join(System.lineSeparator(), lines);
                                showMessageError(parent, message, "Ошибка!");
                                return;
                            }
                        }
                    }

                    ++count;
                }
                ss = 0;
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

    void flashVerify(MainJFrame parent, FlashTableModel tableModel) {
        try (ArduinoCommPort cp = openCommPort()) {
            int count = 0;

            LocalTime start = LocalTime.now();
            blocks:
            for (int b = 0; b < numBlocks; ++b) {
                for (int s = 0; s < numSectors; ++s) {
                    SectorData sd = tableModel.getSectorData(b, s);

                    if (sd.isEmpty()) {
                    } else {
                        ByteArrayOutputStream bs = new ByteArrayOutputStream(4 * 1024);

                        int addr = (b * numSectors + s) * 4 * 1024;

                        for (int i = 0; i < 4; i++) {
                            if (parent.isStop()) {
                                break blocks;
                            }

                            byte[] buf = new byte[1024];

                            parent.setInfo("Сравнение сектора:" + b + ":" + s + ":" + (addr + i * buf.length));
                            cp.writeLine("r" + String.valueOf(addr + i * buf.length));
                            cp.readBlock(buf);

                            String line = cp.readLine();
                            if (line.startsWith("FAIL")) {
                                List<String> lines = new ArrayList<>();
                                lines.add(line);
                                lines.add("Ошибка при сравнении сектора:" + b + ":" + s + ":" + (addr + i * buf.length));
                                lines.add("Проверено секторов " + count);
                                String message = String.join(System.lineSeparator(), lines);
                                showMessageError(parent, message, "Ошибка!");
                                return;
                            }

                            bs.write(buf);
                        }

                        byte[] mbs = sd.getBytes();
                        if (!Arrays.equals(bs.toByteArray(), mbs)) {
                            String message = "Ошибка при сравнении сектора:" + b + ":" + s;
                            showMessageError(parent, message, "Ошибка!");
                            return;
                        }
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
