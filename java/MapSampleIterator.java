import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Created by andreas on 04.11.14.
 */
public class MapSampleIterator<V> implements SampleIterator<V> {
  private final Iterator<Map.Entry<Long, V>> it;
  private final int size;
  private Map.Entry<Long, V> entry;

  public MapSampleIterator(NavigableMap<Long, V> map) {
    if (map == null)
      throw new IllegalArgumentException();
    this.size = map.size();
    this.it = map.entrySet().iterator();
    this.entry = null;
  }

  @Override
  public boolean next() {
    if (it.hasNext()) {
      entry = it.next();
      return true;
    }
    return false;
  }

  @Override
  public long time() {
    return entry.getKey();
  }

  @Override
  public V value() {
    return entry.getValue();
  }
}
