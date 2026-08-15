sed -i '249s/errorMessage = "Sign In Failed: ${getClerkErrorMessage(it)}"/errorMessage = "Please enter both email and password"/' app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt
sed -i '264s/errorMessage = "Sign In Failed: ${getClerkErrorMessage(it)}"/errorMessage = "Sign in incomplete, missing session"/' app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt
