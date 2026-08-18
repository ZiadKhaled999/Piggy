import re

with open('admin.html', 'r') as f:
    content = f.read()

target_widget = """                        const content = decodeURIComponent(escape(atob(data.content)));
                        this.state.config = JSON.parse(content);"""

replacement_widget = """                        const content = decodeURIComponent(escape(atob(data.content))).trim();
                        try {
                            this.state.config = content ? JSON.parse(content) : {};
                        } catch (err) {
                            console.warn("Widget JSON Parse Error:", err);
                            this.state.config = {};
                        }"""

target_notif = """                        const nContent = decodeURIComponent(escape(atob(nData.content)));
                        this.state.notifConfig = JSON.parse(nContent);"""

replacement_notif = """                        const nContent = decodeURIComponent(escape(atob(nData.content))).trim();
                        try {
                            this.state.notifConfig = nContent ? JSON.parse(nContent) : {};
                        } catch (err) {
                            console.warn("Notif JSON Parse Error:", err);
                            this.state.notifConfig = {};
                        }"""

content = content.replace(target_widget, replacement_widget)
content = content.replace(target_notif, replacement_notif)

with open('admin.html', 'w') as f:
    f.write(content)
