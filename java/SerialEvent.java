import java.util.Objects;

/**
 * Created by andreas on 01.10.14.
 */
public class SerialEvent extends FlocklabEvent {

  private SerialDirection direction;
  private String output;


  public SerialEvent(long timestamp, int overseerID, SerialDirection direction, String output) {
    super(timestamp, overseerID);
    if(output == null)
      throw new IllegalArgumentException();

    this.direction = direction;
    this.output = output;
  }

  public SerialDirection getDirection() {
    return direction;
  }

  public String getOutput() {
    return output;
  }

  @Override
  public boolean equals(Object obj) {
    return
        obj == this ||
        (obj.getClass() == this.getClass() && this.equals((SerialEvent) obj));
  }

  public boolean equals(SerialEvent other) {
    return other != null &&
      (
        other == this ||
        (
          super.equals(other) &&
          direction == other.direction &&
          (output == other.output || output.equals(other.output))
        )
      );
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), direction, output);
  }

  @Override
  public String toString() {
    return String.format("%d: overseer=%d, dir=%s: %s",
                         getTimestampTenNanoseconds(), getOverseerID(), getDirection().toString(), getOutput());
  }

  public enum SerialDirection {
    NONE, r, w
  }
}
