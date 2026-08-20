import com.clerk.api.user.User
import java.lang.reflect.Modifier

fun test() {
    println("User methods:")
    User::class.java.methods.forEach { m ->
        if (Modifier.isPublic(m.modifiers)) {
            println(m.name + " " + m.parameterTypes.map { it.name })
        }
    }
}
