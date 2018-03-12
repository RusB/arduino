/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rusb.flashspi;

import gnu.io.CommPortIdentifier;
import java.util.Enumeration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ruslan
 */
public class MainJFrameTest {

    public MainJFrameTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCommPortEnumerator() {
        Enumeration<CommPortIdentifier> en = CommPortIdentifier.getPortIdentifiers();
        while (en.hasMoreElements()) {
            CommPortIdentifier cpi = en.nextElement();
            System.out.print(cpi.getPortType());
            System.out.print('\t');
            System.out.println(cpi.getName());
            System.out.println();
        }
    }

}
