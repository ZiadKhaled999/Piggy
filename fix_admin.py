import re

with open('admin.html', 'r') as f:
    content = f.read()

# 1. Add Tabs to Header Actions
tabs_html = """
        </div>
    </header>

    <div class="header" style="top: 60px; padding: var(--space-2) var(--space-6); background: var(--color-surface-50); border-bottom: 1px solid var(--color-surface-200); justify-content: flex-start; z-index: 29;">
        <button class="btn btn-ghost tab-btn active" onclick="app.switchTab('widgets')" id="tab-widgets">
            <i class="fa-solid fa-puzzle-piece"></i> Widgets
        </button>
        <button class="btn btn-ghost tab-btn" onclick="app.switchTab('notifications')" id="tab-notifications">
            <i class="fa-solid fa-bell"></i> Push Notifications
        </button>
    </div>
"""
content = content.replace('        </div>\n    </header>', tabs_html)


# 2. Add Notifications Section to Main
main_start = '<main class="main">'
notif_section = """
        <!-- WIDGETS SECTION -->
        <div id="section-widgets">
"""
content = content.replace('<div class="toolbar">', notif_section + '\n        <div class="toolbar">')

mascot_grid_end = '        </div>\n'
notif_section_end = """
        </div>

        <!-- NOTIFICATIONS SECTION -->
        <div id="section-notifications" class="hidden">
            <div class="toolbar">
                <div class="toolbar-row" style="justify-content: space-between;">
                    <div>
                        <h2 style="font-size: var(--text-lg); color: var(--color-surface-900);">Push Notifications</h2>
                        <p style="font-size: var(--text-sm); color: var(--color-surface-500);">Send custom push alerts to Free or Premium users.</p>
                    </div>
                    <button class="btn btn-primary" onclick="app.showAddNotifModal()">
                        <i class="fa-solid fa-plus"></i> Compose Notification
                    </button>
                </div>
            </div>

            <div id="notifGrid" class="mascot-grid">
                <!-- Cards rendered here via JS -->
            </div>
        </div>
"""
content = content.replace('        </div>\n    </main>', mascot_grid_end + notif_section_end + '\n    </main>')

# 3. Add Notification Modal
modal_start = '    <!-- Connect GitHub Modal -->'
notif_modal = """
    <!-- Add Notification Modal -->
    <div id="notifModal" class="modal-overlay">
        <div class="modal" style="max-width: 42rem;">
            <div class="modal-header">
                <h2 class="header-title" id="notifModalTitle">Compose Notification</h2>
                <button class="modal-close" onclick="app.closeModals()"><i class="fa-solid fa-times"></i></button>
            </div>
            <div class="modal-form">
                <input type="hidden" id="nId" />
                <div class="form-group">
                    <label>Push Title</label>
                    <input type="text" id="nTitle" class="form-control" placeholder="e.g. 50% Off Flash Sale!" required />
                </div>
                <div class="form-group">
                    <label>Message Body</label>
                    <textarea id="nBody" class="form-control" rows="3" placeholder="Enter the message body here..." required></textarea>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label>Target Audience</label>
                        <select id="nTarget" class="form-control">
                            <option value="all">Everyone (All Users)</option>
                            <option value="free">Free Users Only</option>
                            <option value="pro">Premium Users Only</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Status</label>
                        <select id="nStatus" class="form-control">
                            <option value="active">Active (Sending)</option>
                            <option value="inactive">Inactive (Revoked)</option>
                        </select>
                    </div>
                </div>

                <div class="form-group">
                    <label>Expires At (Optional)</label>
                    <input type="date" id="nExpires" class="form-control" />
                    <span style="font-size: var(--text-xs); color: var(--color-surface-400);">After this date, the notification will not be shown to users who haven't seen it yet.</span>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-ghost" onclick="app.closeModals()">Cancel</button>
                <button class="btn btn-success" onclick="app.saveNotification()">Save Notification</button>
            </div>
        </div>
    </div>
"""
content = content.replace(modal_start, notif_modal + '\n' + modal_start)

# 4. Inject JS State & Logic
js_state = """
            state: {
                config: {
                    widget_config: { categories: [], default_image_url: "" }
                },
                notifConfig: {
                    notifications: []
                },
"""
content = re.sub(r'state: \{\s*config: \{\s*widget_config: \{ categories: \[\], default_image_url: "" \}\s*\},', js_state, content)

js_paths = """
                    branch: localStorage.getItem('ghBranch') || 'main',
                    path: localStorage.getItem('ghPath') || 'piggy_remote_config.json',
                    notifPath: localStorage.getItem('ghNotifPath') || 'notifications_config.json'
"""
content = re.sub(r"branch: localStorage\.getItem\('ghBranch'\) \|\| 'main',\s*path: localStorage\.getItem\('ghPath'\) \|\| 'piggy_remote_config\.json'", js_paths, content)

js_fetch = """
            async fetchConfig() {
                const { owner, repo, path, notifPath, branch } = this.state.ghConfig;
                try {
                    // Fetch Widget Config
                    try {
                        const data = await this.ghRequest(`/repos/${owner}/${repo}/contents/${path}?ref=${branch}`);
                        this.state.fileSha = data.sha;
                        const content = decodeURIComponent(escape(atob(data.content)));
                        this.state.config = JSON.parse(content);
                        if (!this.state.config.widget_config) this.state.config.widget_config = { categories: [] };
                    } catch (e) {
                         if (e.message.includes('Not Found')) {
                            this.state.config = { widget_config: { categories: [] } };
                         } else throw e;
                    }

                    // Fetch Notifications Config
                    try {
                        const nData = await this.ghRequest(`/repos/${owner}/${repo}/contents/${notifPath}?ref=${branch}`);
                        this.state.notifFileSha = nData.sha;
                        const nContent = decodeURIComponent(escape(atob(nData.content)));
                        this.state.notifConfig = JSON.parse(nContent);
                        if (!this.state.notifConfig.notifications) this.state.notifConfig.notifications = [];
                    } catch (e) {
                         if (e.message.includes('Not Found')) {
                            this.state.notifConfig = { notifications: [] };
                         } else throw e;
                    }

                    this.toast('Configurations loaded successfully', 'success');
                    this.renderGrid();
                    this.renderNotifGrid();
                } catch (e) {
                    this.toast(`Error loading config: ${e.message}`, 'error');
                }
            },
"""
content = re.sub(r'async fetchConfig\(\) \{.*?(?=async saveConfigToGitHub\(\))', js_fetch, content, flags=re.DOTALL)


js_save = """
            async saveConfigToGitHub() {
                const { owner, repo, path, notifPath, branch } = this.state.ghConfig;
                const btn = document.getElementById('btnSaveConfig');
                
                btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Publishing...';
                btn.disabled = true;

                try {
                    // Save Widget Config
                    const wContent = btoa(unescape(encodeURIComponent(JSON.stringify(this.state.config, null, 2))));
                    const wBody = { message: "Update Piggy widget config", content: wContent, branch: branch };
                    if (this.state.fileSha) wBody.sha = this.state.fileSha;
                    const wData = await this.ghRequest(`/repos/${owner}/${repo}/contents/${path}`, 'PUT', wBody);
                    this.state.fileSha = wData.content.sha;

                    // Save Notifications Config
                    const nContent = btoa(unescape(encodeURIComponent(JSON.stringify(this.state.notifConfig, null, 2))));
                    const nBody = { message: "Update Piggy notifications config", content: nContent, branch: branch };
                    if (this.state.notifFileSha) nBody.sha = this.state.notifFileSha;
                    const nData = await this.ghRequest(`/repos/${owner}/${repo}/contents/${notifPath}`, 'PUT', nBody);
                    this.state.notifFileSha = nData.content.sha;

                    this.toast('Both configurations published successfully!', 'success');
                } catch (e) {
                    this.toast(`Failed to publish: ${e.message}`, 'error');
                } finally {
                    btn.innerHTML = '<i class="fa-solid fa-cloud-arrow-up"></i> Publish to App';
                    btn.disabled = false;
                }
            },
"""
content = re.sub(r'async saveConfigToGitHub\(\) \{.*?(?=async uploadImage)', js_save, content, flags=re.DOTALL)


js_modal = """
                <div class="form-row">
                    <div class="form-group">
                        <label>Branch</label>
                        <input type="text" id="ghBranch" class="form-control" value="main" />
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Widgets File Path</label>
                        <input type="text" id="ghPath" class="form-control" value="piggy_remote_config.json" />
                    </div>
                    <div class="form-group">
                        <label>Notifications File Path</label>
                        <input type="text" id="ghNotifPath" class="form-control" value="notifications_config.json" />
                    </div>
                </div>
"""
content = re.sub(r'<div class="form-row">\s*<div class="form-group">\s*<label>Branch</label>\s*<input type="text" id="ghBranch".*?</div>\s*</div>', js_modal, content, flags=re.DOTALL)


js_connect = """
            showConnectModal() {
                document.getElementById('ghToken').value = this.state.ghConfig.token;
                document.getElementById('ghOwner').value = this.state.ghConfig.owner;
                document.getElementById('ghRepo').value = this.state.ghConfig.repo;
                document.getElementById('ghBranch').value = this.state.ghConfig.branch;
                document.getElementById('ghPath').value = this.state.ghConfig.path;
                document.getElementById('ghNotifPath').value = this.state.ghConfig.notifPath;
                document.getElementById('connectModal').classList.add('is-open');
            },

            connectGitHub() {
                const token = document.getElementById('ghToken').value.trim();
                const owner = document.getElementById('ghOwner').value.trim();
                const repo = document.getElementById('ghRepo').value.trim();
                const branch = document.getElementById('ghBranch').value.trim();
                const path = document.getElementById('ghPath').value.trim();
                const notifPath = document.getElementById('ghNotifPath').value.trim();

                if (!token || !owner || !repo) {
                    this.toast('Please fill in required GitHub details', 'warning');
                    return;
                }

                this.state.ghConfig = { token, owner, repo, branch, path, notifPath };
                localStorage.setItem('ghToken', token);
                localStorage.setItem('ghOwner', owner);
                localStorage.setItem('ghRepo', repo);
                localStorage.setItem('ghBranch', branch);
                localStorage.setItem('ghPath', path);
                localStorage.setItem('ghNotifPath', notifPath);

                this.updateConnectionStatus();
                this.closeModals();
                this.fetchConfig();
            },
"""
content = re.sub(r'showConnectModal\(\) \{.*?(?=closeModals\(\))', js_connect, content, flags=re.DOTALL)


js_custom_logic = """
            switchTab(tabId) {
                document.querySelectorAll('.tab-btn').forEach(b => {
                    b.classList.toggle('active', b.id === 'tab-' + tabId);
                    if(b.id === 'tab-' + tabId) b.style.color = 'var(--color-brand-600)';
                    else b.style.color = 'var(--color-surface-500)';
                });
                document.getElementById('section-widgets').classList.toggle('hidden', tabId !== 'widgets');
                document.getElementById('section-notifications').classList.toggle('hidden', tabId !== 'notifications');
            },

            renderNotifGrid() {
                const grid = document.getElementById('notifGrid');
                const notifs = this.state.notifConfig.notifications || [];

                if (notifs.length === 0) {
                    grid.innerHTML = `
                        <div class="empty-state" style="grid-column: 1 / -1;">
                            <div style="font-size: 3rem; color: var(--color-surface-300); margin-bottom: 1rem;">
                                <i class="fa-regular fa-bell-slash"></i>
                            </div>
                            <h3 style="font-size: var(--text-lg); color: var(--color-surface-600); margin-bottom: 0.5rem;">No notifications yet</h3>
                            <p style="color: var(--color-surface-400); font-size: var(--text-sm);">Compose a notification to push it to users.</p>
                        </div>
                    `;
                    return;
                }

                grid.innerHTML = notifs.map(n => {
                    const statusClass = n.status === 'active' ? 'badge-active' : 'badge-frozen';
                    const targetBadge = n.target_audience === 'pro' ? '<span style="color:var(--color-warning);"><i class="fa-solid fa-crown"></i> Pro</span>' :
                                      n.target_audience === 'free' ? 'Free Users' : 'All Users';
                    
                    return `
                        <div class="mascot-card" style="min-height: auto;">
                            <div class="mascot-card-body">
                                <div>
                                    <span class="mascot-card-badge ${statusClass}" style="position:relative; top:0; left:0; display:inline-block; margin-bottom:8px;">${n.status}</span>
                                    <h3 class="mascot-card-title">${n.title}</h3>
                                    <p style="font-size: var(--text-sm); color: var(--color-surface-600); margin-top: 4px;">${n.body}</p>
                                </div>
                                <div class="mascot-card-meta" style="margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--color-surface-100);">
                                    <span><strong>Target:</strong> ${targetBadge}</span>
                                    <span><strong>Expires:</strong> ${n.expires_at || 'Never'}</span>
                                    <span style="font-size: 10px; color: var(--color-surface-300);">ID: ${n.id}</span>
                                </div>
                                <div class="mascot-card-actions">
                                    <button class="btn btn-sm btn-ghost" onclick="app.editNotification('${n.id}')">
                                        <i class="fa-solid fa-pen"></i> Edit
                                    </button>
                                    <button class="btn btn-sm btn-ghost" style="color: var(--color-danger);" onclick="app.deleteNotification('${n.id}')">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('');
            },

            showAddNotifModal() {
                document.getElementById('notifModalTitle').textContent = 'Compose Notification';
                document.getElementById('nId').value = 'notif_' + Date.now();
                document.getElementById('nTitle').value = '';
                document.getElementById('nBody').value = '';
                document.getElementById('nTarget').value = 'all';
                document.getElementById('nStatus').value = 'active';
                document.getElementById('nExpires').value = '';
                document.getElementById('notifModal').classList.add('is-open');
            },

            editNotification(id) {
                const notif = this.state.notifConfig.notifications.find(n => n.id === id);
                if (!notif) return;

                document.getElementById('notifModalTitle').textContent = 'Edit Notification';
                document.getElementById('nId').value = notif.id;
                document.getElementById('nTitle').value = notif.title;
                document.getElementById('nBody').value = notif.body;
                document.getElementById('nTarget').value = notif.target_audience || 'all';
                document.getElementById('nStatus').value = notif.status || 'active';
                document.getElementById('nExpires').value = notif.expires_at || '';
                
                document.getElementById('notifModal').classList.add('is-open');
            },

            saveNotification() {
                const id = document.getElementById('nId').value;
                const title = document.getElementById('nTitle').value.trim();
                const body = document.getElementById('nBody').value.trim();
                
                if (!title || !body) {
                    this.toast('Title and Body are required', 'warning');
                    return;
                }

                const notifObj = {
                    id: id,
                    title: title,
                    body: body,
                    target_audience: document.getElementById('nTarget').value,
                    status: document.getElementById('nStatus').value,
                    expires_at: document.getElementById('nExpires').value || null
                };

                let notifs = this.state.notifConfig.notifications;
                const existingIdx = notifs.findIndex(n => n.id === id);
                
                if (existingIdx >= 0) {
                    notifs[existingIdx] = notifObj;
                } else {
                    notifs.unshift(notifObj); // Add to top
                }

                this.closeModals();
                this.renderNotifGrid();
                this.toast('Notification saved locally. Remember to Publish!', 'success');
            },

            deleteNotification(id) {
                if (confirm('Are you sure you want to delete this notification?')) {
                    this.state.notifConfig.notifications = this.state.notifConfig.notifications.filter(n => n.id !== id);
                    this.renderNotifGrid();
                    this.toast('Notification deleted locally.', 'info');
                }
            },
"""

content = content.replace('            renderDaysCheckboxes() {', js_custom_logic + '\n            renderDaysCheckboxes() {')

with open('admin.html', 'w') as f:
    f.write(content)

print("Admin HTML modified successfully.")
