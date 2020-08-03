/**
* Created by andreas on 29.10.14.
*/
public class BoundedTimeValue extends ObservableValue<Long> {
  private final long min;
  private final long max;

  public BoundedTimeValue(long min, long max, long current) {
    super();
    if (min > max)
      throw new IllegalArgumentException();
    this.max = max;
    this.min = min;

    setValue(current);
  }

  @Override
  public void setValue(Long newValue) {
    if (newValue == null)
      throw new IllegalArgumentException();
    super.setValue(Math.max(min, Math.min(max, newValue)));
  }
}
