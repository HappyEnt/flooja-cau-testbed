/**
 * Created by andreas on 22.10.14.
 */
public class TimeValue {
  private final long tensOfNanoSeconds;

  public TimeValue(long tensOfNanoSeconds) {
    this.tensOfNanoSeconds = tensOfNanoSeconds;
  }

  public int remainingNanoSeconds() {
    return (int) ((tensOfNanoSeconds % 100) * 10);
  }

  public int remainingMicroSeconds() {
    return (int) (microSeconds() % 1000);
  }

  public int remainingMilliSeconds() {
    return (int) (milliSeconds() % 1000);
  }

  public long milliSeconds() {
    return microSeconds() / 1000;
  }

  public int remainingSeconds() {
    return (int) seconds() % 60;
  }

  public int remainingMinutes() {
    return (int) (minutes() % 60);
  }

  public long microSeconds() {
    return tensOfNanoSeconds / 100;
  }

  public long seconds() {
    return milliSeconds() / 1000;
  }

  public long minutes() {
    return seconds() / 60;
  }

  public long hours() {
    return minutes() / 60;
  }
}
