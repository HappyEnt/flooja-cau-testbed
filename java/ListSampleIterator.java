import java.util.List;

/**
 * Created by andreas on 04.11.14.
 */
public class ListSampleIterator<V> implements SampleIterator<V> {
  private final List<Long> times;
  private final List<V> values;
  private final int size;
  private int cursor;

  public ListSampleIterator(List<Long> times, List<V> values) {
    if (times == null)
      throw new IllegalArgumentException();
    if (values == null)
      throw new IllegalArgumentException();
    if (times.size() != values.size())
      throw new IllegalArgumentException("times must have same size as values");

    this.times = times;
    this.values = values;
    this.cursor = -1;
    this.size = times.size();
  }

  @Override
  public boolean next() {
    ++cursor;
    if (cursor >= size || cursor < 0) {
      cursor = -2;
      return false;
    }
    return true;
  }

  @Override
  public long time() {
    return times.get(cursor);
  }

  @Override
  public V value() {
    return values.get(cursor);
  }
}
