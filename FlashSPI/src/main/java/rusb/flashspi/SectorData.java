package rusb.flashspi;

import java.util.Objects;

/**
 *
 * @author bryhljaev
 */
public class SectorData {

    static final SectorData UNKNOWN = new SectorData();

    private final byte[] bs;
    private final int len;

    private boolean isEmpty = true;

    private SectorData() {
        bs = null;
        len = 0;
    }

    public SectorData(byte[] bs, int len) {
        this.bs = bs;
        this.len = len;

        for (byte b : bs) {
            if ((b & 0xFF) != 0xFF) {
                isEmpty = false;
                break;
            }
        }
    }

    @Override
    public String toString() {
        if (len == 0 || Objects.isNull(bs)) {
            return "";
        }

        if (len != 4 * 1024) {
            return String.valueOf(len);
        }

        return isEmpty ? "." : "#";
    }

}
