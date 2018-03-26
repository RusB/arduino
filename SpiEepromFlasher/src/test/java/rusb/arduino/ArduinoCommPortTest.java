package rusb.arduino;

import gnu.io.CommPortIdentifier;
import java.io.IOException;
import java.util.Enumeration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ruslan
 */
public class ArduinoCommPortTest {

    private static CommPortIdentifier commPortIdentifier;

    @BeforeClass
    public static void setUpClass() {
        Enumeration<CommPortIdentifier> en = CommPortIdentifier.getPortIdentifiers();
        if (en.hasMoreElements()) {
            commPortIdentifier = en.nextElement();
        }

        assertNotNull("Port not detected.", commPortIdentifier);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    private ArduinoCommPort commPort;

    @Before
    public void setUp() throws IOException {
        commPort = new ArduinoCommPort(commPortIdentifier);
        commPort.open();
        do {
            String line = commPort.readLine();
            if (line.startsWith("Init")) {
                break;
            }
        } while (true);
    }

    @After
    public void tearDown() {
        commPort.close();
    }

    @Test
    public void testGetName() {
        System.out.println("getName");
        assertEquals(
                commPortIdentifier.getName(),
                commPort.getName());
    }

    @Test(timeout = 5000)
    public void testReadLine() throws IOException {
        System.out.println("readLine");

        String line;
        commPort.writeLine("i");

        line = commPort.readLine();
        assertTrue(line.startsWith("DeviceID: 0x"));
        line = commPort.readLine();
        assertTrue(line.startsWith("JEDEC ID: 0x"));
        line = commPort.readLine();
        assertTrue(line.startsWith("Manufacturer ID: 0x"));
        line = commPort.readLine();
        assertTrue(line.startsWith("Memory Type: 0x"));
        line = commPort.readLine();
        assertTrue(line.startsWith("Capacity: 0x"));
        line = commPort.readLine();
        assertTrue(line.startsWith("Capacity: 0x"));
        line = commPort.readLine();
        assertTrue(line.startsWith("OK"));
    }

    @Test(timeout = 10000)
    public void testWriteLine() throws IOException {
        System.out.println("writeLine");

        commPort.writeLine("x5432");
        assertEquals(
                "Unknown command: x 5432",
                commPort.readLine());
        assertEquals(
                "OK // SPI EEPROM FLASHER",
                commPort.readLine());
    }

    @Test(timeout = 5000)
    public void testReadBlock() throws IOException {
        System.out.println("readBlock");

        commPort.writeLine("F");
        byte[] bs = new byte[1024];
        commPort.readBlock(bs);
        assertEquals("OK", commPort.readLine());

        for (int i = 0; i < bs.length; i++) {
            long expected = 0xFF & (i + 1);
            long actual = 0xFF & bs[i];
            assertEquals(
                    String.format("Ошибка в элемнте %d: %d != %d", i, expected, actual),
                    expected, actual);
        }
    }

    @Test(timeout = 60000)
    public void testWriteBlock() throws IOException {
        System.out.println("writeBlock");

        byte[] bs = new byte[1024];

        for (int i = 0; i < bs.length; ++i) {
            bs[i] = (byte) -(i + 1);
        }

        commPort.writeLine("W");
        commPort.writeBlock(bs);
        assertEquals("OK 0x400", commPort.readLine());

        commPort.writeLine("R");
        commPort.readBlock(bs);
        assertEquals("OK", commPort.readLine());

        for (int i = 0; i < bs.length; i++) {
            long expected = 0xFF & -(i + 1);
            long actual = 0xFF & bs[i];
            assertEquals(
                    String.format("Ошибка в элемнте %d: %d != %d", i, expected, actual),
                    expected, actual);
        }
    }

}
