import com.clerk.api.Clerk
import com.clerk.api.user.update

fun test() {
    Clerk.userFlow.value?.update {
        firstName = "Test"
    }
}
