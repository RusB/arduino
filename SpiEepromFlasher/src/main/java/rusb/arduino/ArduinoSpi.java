/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rusb.arduino;

import gnu.io.CommPortIdentifier;
import java.io.IOException;

/**
 *
 * @author ruslan
 */
public abstract class ArduinoSpi {

    protected final ArduinoCommPort commPort;

    public ArduinoSpi(CommPortIdentifier commPortIdentifier) {
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

}
