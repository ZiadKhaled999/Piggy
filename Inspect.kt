import java.io.File
import java.net.URLClassLoader

fun main() {
    val file = File("clerk-api.jar")
    val url = file.toURI().toURL()
    val cl = URLClassLoader(arrayOf(url))
    val clazz = cl.loadClass("com.clerk.api.user.UserKt")
    clazz.methods.forEach { println(it.name) }
}
