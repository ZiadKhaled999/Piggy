#!/bin/bash
sed -i 's/R.drawable.img_app_logo/R.drawable.ic_notification/g' app/src/main/java/com/oryno/piggy_ledger/ui/NotificationHelper.kt
sed -i 's/R.drawable.ic_notification_logo/R.drawable.ic_notification/g' app/src/main/java/com/oryno/piggy_ledger/ui/NotificationHelper.kt
sed -i 's/\.setColorized(true)//g' app/src/main/java/com/oryno/piggy_ledger/ui/NotificationHelper.kt
sed -i 's/AndroidColor\.parseColor("#F43F5E")/androidx.core.content.ContextCompat.getColor(context, R.color.pink_primary)/g' app/src/main/java/com/oryno/piggy_ledger/ui/NotificationHelper.kt
