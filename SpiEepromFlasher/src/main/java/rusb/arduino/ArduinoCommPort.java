package rusb.arduino;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ruslan
 */
public class ArduinoCommPort implements AutoCloseable {

    private final CommPortIdentifier commPortIdentifier;

    private RXTXPort port;

    public ArduinoCommPort(CommPortIdentifier commPortIdentifier) {
        this.commPortIdentifier = commPortIdentifier;
    }

    public String getName() {
        return commPortIdentifier.getName();
    }

    public void open() throws IOException {
        try {
            port = commPortIdentifier.open(getClass().getSimpleName(), 0);
            //port.setEndOfInputChar((byte) '\n');
            port.setSerialPortParams(115200, RXTXPort.DATABITS_8, RXTXPort.STOPBITS_1, RXTXPort.PARITY_NONE);
        } catch (PortInUseException | UnsupportedCommOperationException ex) {
            throw new IOException("Открытие " + commPortIdentifier.getName(), ex);
        }
    }

    @Override
    public void close() {
        port.close();
    }

    public String readLine() throws IOException {
        port.enableReceiveTimeout(1000);

        InputStream inp = port.getInputStream();
        StringBuilder line = new StringBuilder(80);

        do {
            int ch = inp.read();
            switch (ch) {
                case -1:
                case '\r':
                    break;

                case '\n':
                    return line.toString();

                default:
                    line.append((char) ch);
            }
        } while (true);
    }

    public void writeLine(String text) throws IOException {
        OutputStream out = port.getOutputStream();
        out.write(text.getBytes(StandardCharsets.US_ASCII));
        out.write('\n');
        out.flush();
    }

    public int readBlock(byte[] bs) throws IOException {
        port.disableReceiveTimeout();

        InputStream inp = port.getInputStream();
        for (int i = 0; i < bs.length; ++i) {
            int ch = inp.read();
            bs[i] = (byte) ch;
        }

        return bs.length;
    }

    public void writeBlock(byte[] bs) throws IOException {
        port.enableReceiveTimeout(1000);

        InputStream inp = port.getInputStream();
        OutputStream out = port.getOutputStream();

        int off = 0, len = 32;
        do {
            int ch;
            do {
                ch = inp.read();
            } while (ch != '+');

            out.write(bs, off, len);
            out.flush();

            off += len;
            if (off + len > bs.length) {
                len = bs.length - off;
            }
        } while (off < bs.length);
    }

}
