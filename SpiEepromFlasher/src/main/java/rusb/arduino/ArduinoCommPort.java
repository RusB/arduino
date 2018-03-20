package rusb.arduino;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author ruslan
 */
public class ArduinoCommPort implements AutoCloseable {

    private final CommPortIdentifier commPortIdentifier;

    private RXTXPort port;
    private BufferedInputStream inp;
    private BufferedOutputStream out;

    ArduinoCommPort(CommPortIdentifier commPortIdentifier) {
        this.commPortIdentifier = commPortIdentifier;
    }

    public String getName() {
        return commPortIdentifier.getName();
    }

    void open() throws IOException {
        try {
            port = commPortIdentifier.open(getClass().getSimpleName(), 0);
            //port.setEndOfInputChar((byte) '\n');
            port.setSerialPortParams(115200, RXTXPort.DATABITS_8, RXTXPort.STOPBITS_1, RXTXPort.PARITY_NONE);

            inp = new BufferedInputStream(port.getInputStream());
            out = new BufferedOutputStream(port.getOutputStream());
        } catch (PortInUseException | UnsupportedCommOperationException ex) {
            throw new IOException("Открытие " + commPortIdentifier.getName(), ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
            inp.close();
        } finally {
            port.close();
        }
    }

    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder();
        int ch;
        while ((ch = inp.read()) != -1) {
            if (ch != '\n') {
                if (ch != '\r') {
                    line.append((char) ch);
                }
            } else {
                break;
            }
        }
//        System.out.print(" >");
//        System.out.println(line);
        return line.toString();
    }

    public void writeLine(String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.US_ASCII));
        out.write('\n');
        out.flush();
    }

    public int readBlock(int addr, byte[] bs) throws IOException {
        writeLine("r" + String.valueOf(addr));

        for (int i = 0; i < bs.length; i++) {
            int ch = inp.read();
            bs[i] = (byte) ch;
        }

        return bs.length;
    }

    public void writeBlock(int addr, byte[] bs) throws IOException {
        writeLine("w" + String.valueOf(addr));
        out.write(bs);
        out.flush();
    }

}
