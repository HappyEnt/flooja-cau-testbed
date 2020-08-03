/**
 * Created by andreas on 08.10.14.
 */
public interface FlocklabNode {
  int getId();
  SerialEvent [] getSerialOutputs();
  GpioEvent [] getGpioTrace();
}
