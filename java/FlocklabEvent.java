import java.util.Comparator;

/**
 * Created by andreas on 01.10.14.
 */
public abstract class FlocklabEvent {
  public static final Comparator<FlocklabEvent> ORDER = new Comparator<FlocklabEvent>() {
    public int compare(FlocklabEvent e1, FlocklabEvent e2) {
      return Long.compare(e1.getTimestampTenNanoseconds(), e2.getTimestampTenNanoseconds());
    }
  };
  /**
   * Time of event in tens of nanoseconds since epoch (jan 1st 1970)
   */
  private final long timestamp;
  private final int overseerID;

  /**
   *
   * @param timestamp time of event in tens of nanoseconds since Jan 1st 1970
   */
  public FlocklabEvent(long timestamp, int overseerID) {
    this.timestamp = timestamp;
    this.overseerID = overseerID;
  }

  /**
   * Get the time of the event.
   * Measured in tens of nanoseconds since epoch (Jan 1st 1907)
   */
  public long getTimestampTenNanoseconds() {
    return timestamp;
  }

  /**
   * Get the millisecond part of the timestamp with the rest truncated.
   * @return Time of the event in milliseconds
   */
  public long getTimestampMillis() {
    return timestamp / 100000;
  }

  public long getTimestampSeconds() {
    return timestamp / 100000000;
  }

  public int getTimestampSubSecondsNanoseconds() {
    return (int) (timestamp % 100000000) * 10;
  }

  public int getOverseerID() {
    return overseerID;
  }

  public boolean equals(FlocklabEvent other) {
    return other != null &&
        (other == this ||
        (
          timestamp == other.timestamp &&
          overseerID == other.overseerID
        ));
  }

  @Override
  public String toString() {
    return String.format("%d, overseer=%d", getTimestampTenNanoseconds(), getOverseerID());
  }
}
