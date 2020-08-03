/**
 * Created by andreas on 30.10.14.
 */
public interface SampleIterator<V> {
  boolean next();
  long time();
  V value();
}
