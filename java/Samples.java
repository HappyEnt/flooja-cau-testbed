import java.util.List;

/**
 * Created by andreas on 11.11.14.
 */
public class Samples<T> implements IterableSamples<T> {
  private final List<Long> times;
  private final List<T> values;

  public Samples(List<Long> times, List<T> values) {
    if (times == null)
      throw new IllegalArgumentException();
    if (values == null)
      throw new IllegalArgumentException();
    if (times.size() != values.size())
      throw new IllegalArgumentException("times and values must contain same number of elements");

    this.times = times;
    this.values = values;
  }

  public List<Long> getTimes() {
    return times;
  }

  public List<T> getValues() {
    return values;
  }

  public SampleIterator<T> getIterator() {
    return new ListSampleIterator<T>(times, values);
  }
}
