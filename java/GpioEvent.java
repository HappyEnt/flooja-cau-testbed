/**
 * Created by andreas on 08.10.14.
 */
public class GpioEvent extends FlocklabEvent {
  private final String pinName;
  private final boolean value;

  /**
   *
   * @param timestamp
   * @param observerId
   * @param nodeId
   * @param pinName
   * @param value
   */
  public GpioEvent(long timestamp, int overseerID, String pinName, boolean value) {
    super(timestamp, overseerID);

    if (pinName == null)
      throw new IllegalArgumentException("pinname cannot be null");

    this.pinName = pinName;
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    return
        obj == this ||
        (
          obj.getClass() == this.getClass() &&
          this.equals((GpioEvent) obj)
        );
  }

  public boolean equals(GpioEvent other) {
    return other != null &&
      (
        other == this ||
        (
          super.equals(other) &&
          (pinName == other.pinName || pinName.equals(other.pinName)) &&
          value == other.value
        )
      );
  }

  public String getPinName() {
    return pinName;
  }

  public boolean isPositiveFlank() {
    return value;
  }

  @Override
  public String toString() {
    return super.toString() + String.format(", pin=%s, val=%b", getPinName(), isPositiveFlank());
  }
}
