import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.model.error.ClerkErrorResponse

fun getErrorMessage(err: Any?): String {
    if (err is ClerkResult.Failure<*>) {
        val errorResponse = err.error as? ClerkErrorResponse
        val clerkError = errorResponse?.errors?.firstOrNull()
        return clerkError?.longMessage ?: clerkError?.message ?: err.throwable?.message ?: "Unknown error"
    }
    return err?.toString() ?: "Unknown error"
}
