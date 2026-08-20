import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class Inspect {
    public static void main(String[] args) throws Exception {
        File file = new File("clerk-api.jar");
        URL url = file.toURI().toURL();
        URLClassLoader cl = new URLClassLoader(new URL[]{url});
        Class<?> clazz = cl.loadClass("com.clerk.api.user.UserKt");
        for (Method m : clazz.getMethods()) {
            System.out.println(m.getName());
        }
    }
}
