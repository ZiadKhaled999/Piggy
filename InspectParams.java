import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class InspectParams {
    public static void main(String[] args) throws Exception {
        File file = new File("clerk-api.jar");
        File kotlinStdlib = new File("/opt/gradle/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.10/8b80e2f80a3b7daa99c16017f91d4c72cd8b31a6/kotlin-stdlib-2.3.10.jar");
        URL url = file.toURI().toURL();
        URL kurl = kotlinStdlib.toURI().toURL();
        URLClassLoader cl = new URLClassLoader(new URL[]{url, kurl});
        Class<?> clazz = cl.loadClass("com.clerk.api.user.User$UpdateParams");
        for (Method m : clazz.getMethods()) {
            System.out.println(m.getName());
        }
    }
}
