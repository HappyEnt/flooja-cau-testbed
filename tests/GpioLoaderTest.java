import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

public class GpioLoaderTest {
  @Test
  public void testGpioLoader() throws Exception {
    Reader reader = new StringReader(
        "# timestamp,observer_id,node_id,pin_name,value\n" +
        "1234567999.11145678,13,13,LED2,0\n" +
               "123.45678,13,13,LED2,1\n" +
               "123.45679,11,11,INT1,1"
    );
    GpioLoader gpioLoader = new GpioLoader(reader);
    gpioLoader.load();
    Map<Integer, Map<String, List<GpioEvent>>> actual = gpioLoader.getEvents();

    Map<Integer, Map<String, List<GpioEvent>>> expected = new HashMap<Integer, Map<String, List<GpioEvent>>>();

    Map<String, List<GpioEvent>> node13Events = new HashMap<String, List<GpioEvent>>();
    node13Events.put("LED2", Arrays.asList(
        new GpioEvent(12345678000L, 13, 13, "LED2", true),
        new GpioEvent(123456799911145678L, 13, 13, "LED2", false)));

    Map<String, List<GpioEvent>> node11Events = new HashMap<String, List<GpioEvent>>();
    node11Events.put("INT1", Arrays.asList(new GpioEvent(12345679000L, 11, 11, "INT1", true)));

    expected.put(13, node13Events);
    expected.put(11, node11Events);

    assertEquals(expected, actual);
  }
}