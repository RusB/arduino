package rusb.flashspi;

import gnu.io.CommPortIdentifier;

/**
 *
 * @author ruslan
 */
class CommPortModel {

    final CommPortIdentifier cpi;

    public CommPortModel(CommPortIdentifier cpi) {
        this.cpi = cpi;
    }

    @Override
    public String toString() {
        return cpi.getName();
    }

}
