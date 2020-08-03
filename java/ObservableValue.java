import java.util.Objects;
import java.util.Observable;

/**
 * Created by andreas on 20.10.14.
 */
public class ObservableValue<T> extends Observable {
  private T value;

  public ObservableValue() {
    super();
  }

  public ObservableValue(T value) {
    super();
    this.value = value;
  }

  public void setValue(T newValue) {
    T old = value;
    this.value = newValue;
    if (!Objects.equals(old, newValue)) {
      setChanged();
      notifyObservers();
    }
  }

  public T getValue() {
    return this.value;
  }
}
