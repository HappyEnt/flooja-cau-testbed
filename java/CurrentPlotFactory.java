/**
 * Created by andreas on 09.11.14.
 */
public class CurrentPlotFactory {
  private final BoundedTimeValue currentTime;
  private final ObservableValue<Double> maxCurrent;

  public CurrentPlotFactory(BoundedTimeValue currentTime) {
    if (currentTime == null)
      throw new IllegalArgumentException();

    this.currentTime = currentTime;
    this.maxCurrent = new ObservableValue<Double>(35d);
  }

  public CurrentPlot makeCurrentPlot(CurrentTrace trace) {
    return new CurrentPlot(trace, currentTime, maxCurrent);
  }

  public ObservableValue<Double> getMaxCurrent() {
    return maxCurrent;
  }
}
