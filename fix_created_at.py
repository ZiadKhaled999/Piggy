import re

with open('admin.html', 'r') as f:
    content = f.read()

# Update saveMascot to include created_at
content = content.replace(
    "                    days_of_week: daysOfWeek\n                };",
    "                    days_of_week: daysOfWeek,\n                    created_at: Date.now()\n                };"
)

# Update saveNotification to include created_at
content = content.replace(
    "                    expires_at: document.getElementById('nExpires').value || null\n                };",
    "                    expires_at: document.getElementById('nExpires').value || null,\n                    created_at: Date.now()\n                };"
)

with open('admin.html', 'w') as f:
    f.write(content)
