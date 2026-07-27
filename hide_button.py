with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

replacement = """
            if (currentPage !in 7..9) {
                // Custom Progress Button
                ProgressNextButton(
                    currentPage = currentPage,
                    totalPages = pages.size,
                    isSmallScreen = isSmallScreen,
                    onNext = {
                        if (currentPage < pages.size - 1) {
                            if (currentPage == 3) {
                                requestSmsPermissions()
                            } else if (currentPage == 4) {
                                requestNotificationPermissions()
                            } else if (currentPage == 5 && selectedIntent == -1) {
                                Toast.makeText(context, context.getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
                            } else if (currentPage == 6 && selectedIntensity == -1) {
                                Toast.makeText(context, context.getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
                            } else {
                                currentPage++
                            }
                        } else {
                            onComplete(selectedIntent, selectedIntensity, selectedSavingMode)
                        }
                    }
                )
            }
"""

content = content.replace(
"""            // Custom Progress Button
            ProgressNextButton(
                currentPage = currentPage,
                totalPages = pages.size,
                isSmallScreen = isSmallScreen,
                onNext = {
                    if (currentPage < pages.size - 1) {
                        if (currentPage == 3) {
                            requestSmsPermissions()
                        } else if (currentPage == 4) {
                            requestNotificationPermissions()
                        } else if (currentPage == 5 && selectedIntent == -1) {
                            Toast.makeText(context, context.getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
                        } else if (currentPage == 6 && selectedIntensity == -1) {
                            Toast.makeText(context, context.getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
                        } else {
                            currentPage++
                        }
                    } else {
                        onComplete(selectedIntent, selectedIntensity, selectedSavingMode)
                    }
                }
            )""", replacement)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
