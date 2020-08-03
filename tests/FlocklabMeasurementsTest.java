import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class FlocklabMeasurementsTest {
  @Test
  public void testConstructor() throws Exception {
    File folder = new File("dummymeasurements");

    FlocklabMeasurements measurements = new FlocklabMeasurements(folder);

    assertEquals(measurements.getSerialOutputFile(), new File(folder, "serial.csv"));
    assertEquals(measurements.getGpioTraceFile(), new File(folder, "gpiotracing.csv"));
  }
}
