package rusb.flashspi;

/**
 *
 * @author bryhljaev
 */
public class SectorData {

    static final SectorData UNKNOWN = new SectorData(new byte[0]) {
        @Override
        public String toString() {
            return ".";
        }
    };

    private final byte[] bs;

    private final boolean isEmpty;

    public SectorData(byte[] bs) {
        this.bs = bs;
        this.isEmpty = check(bs);
    }

    private boolean check(byte[] bs) {
        for (byte b : bs) {
            if (b != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return isEmpty ? "=" : "#";
    }

    public byte[] getBytes() {
        return bs;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

}
