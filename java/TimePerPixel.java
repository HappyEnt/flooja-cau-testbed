/**
 * Created by andreas on 20.10.14.
 */
class TimePerPixel extends ObservableValue<Double> {
  public TimePerPixel(double timePerPixel) {
    super(timePerPixel);

    if (timePerPixel < MIN || timePerPixel > MAX)
      throw new IllegalArgumentException();
  }

  public final double getNextLowerValue() {
    return cropToMinMax(getValue() / 1.2d);
  }
  public final double getNextHigherValue() {
    return cropToMinMax(getValue() * 1.2d);
  }
  public final void decrease() {
    setValue(getNextLowerValue());
  }
  public final void increase() {
    setValue(getNextHigherValue());
  }


  // at most 50 px per micro second
  private static final double MIN = 2d;
  // at most 6 min per 2000px
  private static final double MAX = 18e6;


  private double cropToMinMax(double value) {
    return Math.max(MIN, Math.min(MAX, value));
  }


  // assertions
  static {
    assert MAX > 0;
    assert MIN > 0;
    assert MIN <= MAX;
  }
}
