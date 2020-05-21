import com.hangyin.smart.smartcore.plugin.mqtt.MQTTService;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hang.yin
 * @date 2020-05-19 13:33
 */
public class serviceTest {

    public static void main(String[] args) {
        try(InputStream cfgIn = Files.newInputStream(Paths.get("config", "CC50E3CC8994.yaml"))) {
            Map cfg = new Yaml().load(cfgIn);
            MQTTService service = new MQTTService();
            service.init(new HashMap<>(), (did, pid, nv, ov) -> {
                System.out.println(did + ", " + pid + ", " + nv + "," + ov);
                return true;
            }, "CC50E3CC8994", cfg);

            System.out.println("123");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
