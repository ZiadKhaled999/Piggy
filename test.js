        const app = {
            
            state: {
                config: {
                    widget_config: { categories: [], default_image_url: "" }
                },
                notifConfig: {
                    notifications: []
                },

                ghConfig: {
                    token: localStorage.getItem('ghToken') || '',
                    owner: localStorage.getItem('ghOwner') || 'ZiadKhaled999',
                    repo: localStorage.getItem('ghRepo') || 'piggy-assets',
                    
                    branch: localStorage.getItem('ghBranch') || 'main',
                    path: localStorage.getItem('ghPath') || 'piggy_remote_config.json',
                    notifPath: localStorage.getItem('ghNotifPath') || 'notifications_config.json'

                },
                fileSha: null,
                filter: 'all',
                searchQuery: '',
                sort: 'name_asc',
                editingId: null,
                chartInstances: {},
                pendingImageUpload: null
            },

            init() {
                this.updateConnectionStatus();
                this.renderDaysCheckboxes();
                if (this.state.ghConfig.token && this.state.ghConfig.owner && this.state.ghConfig.repo) {
                    this.fetchConfig();
                } else {
                    this.showConnectModal();
                }
            },

            toast(message, type = 'info') {
                const container = document.getElementById('toastContainer');
                const toast = document.createElement('div');
                toast.className = `toast ${type}`;
                
                const iconMap = {
                    success: 'fa-check-circle',
                    error: 'fa-exclamation-circle',
                    info: 'fa-info-circle'
                };
                
                toast.innerHTML = `
                    <i class="fa-solid ${iconMap[type]} toast-icon"></i>
                    <span>${message}</span>
                `;
                
                container.appendChild(toast);
                
                setTimeout(() => {
                    toast.classList.add('hiding');
                    toast.addEventListener('animationend', () => toast.remove());
                }, 3000);
            },

            updateConnectionStatus() {
                const badge = document.getElementById('githubStatus');
                const saveBtn = document.getElementById('btnSaveConfig');
                const pullBtn = document.getElementById('btnPullConfig');
                if (this.state.ghConfig.token) {
                    badge.classList.add('is-connected');
                    badge.innerHTML = '<span class="dot"></span> Connected';
                    saveBtn.disabled = false;
                    if (pullBtn) pullBtn.disabled = false;
                } else {
                    badge.classList.remove('is-connected');
                    badge.innerHTML = '<span class="dot"></span> Not Connected';
                    saveBtn.disabled = true;
                    if (pullBtn) pullBtn.disabled = true;
                }
            },

            async ghRequest(endpoint, method = 'GET', body = null) {
                const { token } = this.state.ghConfig;
                const headers = {
                    'Accept': 'application/vnd.github.v3+json',
                    'Authorization': `token ${token}`
                };
                const options = { method, headers };
                if (body) {
                    options.body = JSON.stringify(body);
                    headers['Content-Type'] = 'application/json';
                }
                const res = await fetch(`https://api.github.com${endpoint}`, options);
                if (!res.ok) {
                    const err = await res.json();
                    throw new Error(err.message || 'GitHub API request failed');
                }
                return res.json();
            },

            
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
async uploadImage(file, mascotId) {
                const { owner, repo, branch } = this.state.ghConfig;
                const ext = file.name.split('.').pop();
                const filename = `assets/mascots/${mascotId}_${Date.now()}.${ext}`;
                
                return new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = async () => {
                        const base64 = reader.result.split(',')[1];
                        try {
                            const data = await this.ghRequest(`/repos/${owner}/${repo}/contents/${filename}`, 'PUT', {
                                message: `Upload image for mascot ${mascotId}`,
                                content: base64,
                                branch: branch
                            });
                            resolve(data.content.download_url);
                        } catch (e) {
                            reject(e);
                        }
                    };
                    reader.onerror = reject;
                    reader.readAsDataURL(file);
                });
            },

            
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

            closeModals() {
                document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('is-open'));
                this.state.editingId = null;
                this.state.pendingImageUpload = null;
                document.getElementById('mImageFileName').textContent = '';
                document.getElementById('mImageFile').value = '';
            },

            getAllMascots() {
                const cats = this.state.config.widget_config?.categories || [];
                let all = [];
                cats.forEach(c => {
                    (c.mascots || []).forEach(m => {
                        all.push({
                            ...m,
                            _statusKey: c.status_key,
                            _statements: c.statements || []
                        });
                    });
                });
                return all;
            },

            setFilter(filter) {
                this.state.filter = filter;
                document.querySelectorAll('.filter-pill').forEach(el => {
                    el.classList.toggle('is-active', el.dataset.filter === filter);
                });
                this.renderGrid();
            },

            handleSearch(e) {
                this.state.searchQuery = e.target.value.toLowerCase();
                const clearBtn = document.getElementById('searchClear');
                if (this.state.searchQuery) {
                    clearBtn.classList.add('visible');
                } else {
                    clearBtn.classList.remove('visible');
                }
                this.renderGrid();
            },

            clearSearch() {
                document.getElementById('searchInput').value = '';
                this.state.searchQuery = '';
                document.getElementById('searchClear').classList.remove('visible');
                this.renderGrid();
            },

            handleSort(e) {
                this.state.sort = e.target.value;
                this.renderGrid();
            },

            renderGrid() {
                const grid = document.getElementById('mascotGrid');
                let mascots = this.getAllMascots();

                // Apply Status Filter
                if (this.state.filter !== 'all') {
                    mascots = mascots.filter(m => m._statusKey === this.state.filter);
                }

                // Apply Search Filter
                if (this.state.searchQuery) {
                    const q = this.state.searchQuery;
                    mascots = mascots.filter(m => 
                        m.name.toLowerCase().includes(q) || 
                        m.id.toLowerCase().includes(q)
                    );
                }

                // Apply Sort
                mascots.sort((a, b) => {
                    if (this.state.sort === 'name_asc') return a.name.localeCompare(b.name);
                    if (this.state.sort === 'name_desc') return b.name.localeCompare(a.name);
                    return 0; // Newest logic would need created_at field
                });

                if (mascots.length === 0) {
                    grid.innerHTML = `
                        <div class="empty-state" style="grid-column: 1 / -1;">
                            <div style="font-size: 3rem; color: var(--color-surface-300); margin-bottom: 1rem;">
                                <i class="fa-solid fa-ghost"></i>
                            </div>
                            <h3 style="font-size: var(--text-lg); color: var(--color-surface-600); margin-bottom: 0.5rem;">No mascots found</h3>
                            <p style="color: var(--color-surface-400); font-size: var(--text-sm);">Try adjusting your filters or add a new mascot.</p>
                        </div>
                    `;
                    return;
                }

                grid.innerHTML = mascots.map(m => {
                    const badgeClass = `badge-${m._statusKey}`;
                    const daysMap = {1:'Mon',2:'Tue',3:'Wed',4:'Thu',5:'Fri',6:'Sat',7:'Sun'};
                    const daysStr = m.days_of_week?.length ? m.days_of_week.map(d => daysMap[d]).join(', ') : 'All Days';
                    const phraseCount = m._statements.length;

                    return `
                        <div class="mascot-card">
                            <div class="mascot-card-image">
                                <span class="mascot-card-badge ${badgeClass}">${m._statusKey}</span>
                                ${m.image_url ? 
                                    `<img src="${m.image_url.includes('github') ? 'https://piggy-api-endpoints.vercel.app/mascots/' + m.image_url.split('/').pop() : m.image_url}" alt="${m.name}" loading="lazy" />` : 
                                    `<span class="no-image-placeholder">No Image Configured</span>`
                                }
                            </div>
                            <div class="mascot-card-body">
                                <div>
                                    <h3 class="mascot-card-title">${m.name}</h3>
                                    <span class="mascot-card-id">#${m.id}</span>
                                </div>
                                <div class="mascot-card-meta">
                                    <span><i class="fa-regular fa-clock"></i> ${m.start_time} - ${m.end_time}</span>
                                    <span><i class="fa-regular fa-calendar-days"></i> ${daysStr}</span>
                                </div>
                                <div class="mascot-card-phrases">
                                    <span class="label">${phraseCount} Phrases</span>
                                </div>
                                <div class="mascot-card-actions">
                                    <button class="btn btn-sm btn-ghost" onclick="app.editMascot('${m.id}')">
                                        <i class="fa-solid fa-pen"></i> Edit
                                    </button>
                                    <button class="btn btn-sm btn-ghost" style="color: var(--color-danger);" onclick="app.deleteMascot('${m.id}')">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('');
            },


            switchTab(tabId) {
                document.querySelectorAll('.tab-btn').forEach(b => {
                    b.classList.toggle('active', b.id === 'tab-' + tabId);
                    if(b.id === 'tab-' + tabId) b.style.color = 'var(--color-brand-600)';
                    else b.style.color = 'var(--color-surface-500)';
                });
                document.getElementById('section-widgets').classList.toggle('hidden', tabId !== 'widgets');
                document.getElementById('section-notifications').classList.toggle('hidden', tabId !== 'notifications');
                document.getElementById('section-analytics').classList.toggle('hidden', tabId !== 'analytics');

                if (tabId === 'analytics') {
                    this.renderAnalytics();
                }
            },

                        renderAnalytics() {
                const mascots = this.getAllMascots();
                const notifs = this.state.notifConfig.notifications || [];
                
                const period = document.getElementById('analyticsPeriod').value;
                const now = Date.now();
                const cutoff = period === 'all' ? 0 : now - (parseInt(period) * 24 * 60 * 60 * 1000);

                const getCreatedAt = (item) => {
                    if (item.created_at) return item.created_at;
                    if (item.id && item.id.startsWith('notif_')) {
                        const ts = parseInt(item.id.replace('notif_', ''));
                        if (!isNaN(ts) && ts > 1000000000000) return ts;
                    }
                    return now; 
                };

                const filteredMascots = mascots.filter(m => getCreatedAt(m) >= cutoff);
                const filteredNotifs = notifs.filter(n => getCreatedAt(n) >= cutoff);

                document.getElementById('stat-total-widgets').textContent = filteredMascots.length;
                document.getElementById('stat-total-notifs').textContent = filteredNotifs.length;
                
                const activeWidgets = filteredMascots.filter(m => m._statusKey === 'active').length;
                document.getElementById('stat-active-widgets').textContent = activeWidgets;
                
                const activeNotifs = filteredNotifs.filter(n => n.status === 'active').length;
                document.getElementById('stat-active-notifs').textContent = activeNotifs;

                const destroyChart = (id) => {
                    if (this.state.chartInstances[id]) {
                        this.state.chartInstances[id].destroy();
                    }
                };

                // Chart 1: Widget Status
                destroyChart('chartWidgetStatus');
                const widgetStatuses = filteredMascots.reduce((acc, m) => {
                    acc[m._statusKey] = (acc[m._statusKey] || 0) + 1;
                    return acc;
                }, {});
                this.state.chartInstances['chartWidgetStatus'] = new Chart(document.getElementById('chartWidgetStatus').getContext('2d'), {
                    type: 'pie',
                    data: {
                        labels: Object.keys(widgetStatuses),
                        datasets: [{
                            data: Object.values(widgetStatuses),
                            backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444']
                        }]
                    },
                    options: { maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
                });

                // Chart 2: Notif Audience
                destroyChart('chartNotifAudience');
                const notifAudience = filteredNotifs.reduce((acc, n) => {
                    const aud = n.target_audience || 'all';
                    acc[aud] = (acc[aud] || 0) + 1;
                    return acc;
                }, {});
                this.state.chartInstances['chartNotifAudience'] = new Chart(document.getElementById('chartNotifAudience').getContext('2d'), {
                    type: 'doughnut',
                    data: {
                        labels: Object.keys(notifAudience),
                        datasets: [{
                            data: Object.values(notifAudience),
                            backgroundColor: ['#6366f1', '#ec4899', '#8b5cf6']
                        }]
                    },
                    options: { maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
                });

                // Chart 3: Compare Widgets vs Notifications
                destroyChart('chartCompare');
                this.state.chartInstances['chartCompare'] = new Chart(document.getElementById('chartCompare').getContext('2d'), {
                    type: 'bar',
                    data: {
                        labels: ['Widgets', 'Notifications'],
                        datasets: [{
                            label: 'Total Count',
                            data: [filteredMascots.length, filteredNotifs.length],
                            backgroundColor: ['#ef4444', '#f59e0b']
                        }]
                    },
                    options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
                });

                // Chart 4: Time Series
                destroyChart('chartTime');
                let daysToShow = period === 'all' ? 30 : parseInt(period);
                if (daysToShow > 30) daysToShow = 30; 

                const timeLabels = [];
                const widgetData = new Array(daysToShow).fill(0);
                const notifData = new Array(daysToShow).fill(0);
                
                for (let i = daysToShow - 1; i >= 0; i--) {
                    const d = new Date(now - i * 24 * 60 * 60 * 1000);
                    timeLabels.push(d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }));
                }

                const binByDate = (items, dataArr) => {
                    items.forEach(item => {
                        const ts = getCreatedAt(item);
                        const daysAgo = Math.floor((now - ts) / (24 * 60 * 60 * 1000));
                        if (daysAgo >= 0 && daysAgo < daysToShow) {
                            const idx = daysToShow - 1 - daysAgo;
                            dataArr[idx]++;
                        }
                    });
                };
                
                binByDate(filteredMascots, widgetData);
                binByDate(filteredNotifs, notifData);

                this.state.chartInstances['chartTime'] = new Chart(document.getElementById('chartTime').getContext('2d'), {
                    type: 'line',
                    data: {
                        labels: timeLabels,
                        datasets: [
                            {
                                label: 'New Widgets',
                                data: widgetData,
                                borderColor: '#ef4444',
                                backgroundColor: 'rgba(239, 68, 68, 0.1)',
                                fill: true,
                                tension: 0.3
                            },
                            {
                                label: 'New Notifications',
                                data: notifData,
                                borderColor: '#f59e0b',
                                backgroundColor: 'rgba(245, 158, 11, 0.1)',
                                fill: true,
                                tension: 0.3
                            }
                        ]
                    },
                    options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } } }
                });
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
                    expires_at: document.getElementById('nExpires').value || null,
                    created_at: Date.now()
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

            renderDaysCheckboxes() {
                const container = document.getElementById('mDaysGroup');
                const days = [
                    { id: 1, label: 'Mo' }, { id: 2, label: 'Tu' }, { id: 3, label: 'We' },
                    { id: 4, label: 'Th' }, { id: 5, label: 'Fr' }, { id: 6, label: 'Sa' }, { id: 7, label: 'Su' }
                ];
                container.innerHTML = days.map(d => `
                    <div>
                        <input type="checkbox" id="day_${d.id}" value="${d.id}" />
                        <label for="day_${d.id}">${d.label}</label>
                    </div>
                `).join('');
            },

            showAddModal() {
                this.state.editingId = null;
                document.getElementById('editModalTitle').textContent = 'Add New Mascot';
                document.getElementById('mId').value = '';
                document.getElementById('mId').readOnly = false;
                document.getElementById('mName').value = '';
                document.getElementById('mStatus').value = 'active';
                document.getElementById('mImageUrl').value = '';
                document.getElementById('mStartTime').value = '00:00';
                document.getElementById('mEndTime').value = '23:59';
                document.getElementById('mStatements').value = '';
                
                document.querySelectorAll('#mDaysGroup input').forEach(el => el.checked = false);
                this.state.pendingImageUpload = null;
                document.getElementById('mImageFileName').textContent = '';
                
                document.getElementById('editModal').classList.add('is-open');
            },

            editMascot(id) {
                const mascot = this.getAllMascots().find(m => m.id === id);
                if (!mascot) return;

                this.state.editingId = id;
                document.getElementById('editModalTitle').textContent = 'Edit Mascot';
                
                const idField = document.getElementById('mId');
                idField.value = mascot.id;
                idField.readOnly = true; 

                document.getElementById('mName').value = mascot.name;
                document.getElementById('mStatus').value = mascot._statusKey;
                document.getElementById('mImageUrl').value = mascot.image_url || '';
                document.getElementById('mStartTime').value = mascot.start_time || '00:00';
                document.getElementById('mEndTime').value = mascot.end_time || '23:59';
                document.getElementById('mStatements').value = (mascot._statements || []).join('\n');
                
                document.querySelectorAll('#mDaysGroup input').forEach(el => {
                    el.checked = (mascot.days_of_week || []).includes(parseInt(el.value));
                });

                document.getElementById('editModal').classList.add('is-open');
            },

            handleImageSelect(e) {
                const file = e.target.files[0];
                if (file) {
                    this.state.pendingImageUpload = file;
                    document.getElementById('mImageFileName').textContent = file.name;
                    // Clear the manual URL field to avoid confusion
                    document.getElementById('mImageUrl').value = '';
                }
            },

            async saveMascot() {
                const id = document.getElementById('mId').value.trim();
                const name = document.getElementById('mName').value.trim();
                const statusKey = document.getElementById('mStatus').value;
                let imageUrl = document.getElementById('mImageUrl').value.trim();
                const startTime = document.getElementById('mStartTime').value;
                const endTime = document.getElementById('mEndTime').value;
                
                const statementsStr = document.getElementById('mStatements').value;
                const statements = statementsStr.split('\n').map(s => s.trim()).filter(s => s);
                
                const daysOfWeek = Array.from(document.querySelectorAll('#mDaysGroup input:checked')).map(el => parseInt(el.value));

                if (!id || !name) {
                    this.toast('ID and Name are required', 'warning');
                    return;
                }

                // Handle Image Upload if pending
                if (this.state.pendingImageUpload) {
                    this.toast('Uploading image to GitHub...', 'info');
                    try {
                        imageUrl = await this.uploadImage(this.state.pendingImageUpload, id);
                        this.toast('Image uploaded successfully', 'success');
                    } catch (e) {
                        this.toast(`Image upload failed: ${e.message}`, 'error');
                        return; // Stop save if upload fails
                    }
                }

                let cats = this.state.config.widget_config.categories;
                
                // If editing, remove old instance first
                if (this.state.editingId) {
                    cats.forEach(c => {
                        c.mascots = (c.mascots || []).filter(m => m.id !== this.state.editingId);
                    });
                }

                // Find or create category
                let cat = cats.find(c => c.status_key === statusKey);
                if (!cat) {
                    cat = { status_key: statusKey, statements: [], mascots: [] };
                    cats.push(cat);
                }

                // Update category statements
                cat.statements = statements;

                // Build mascot object
                const mascotObj = {
                    id, name, 
                    start_time: startTime, 
                    end_time: endTime,
                    days_of_week: daysOfWeek,
                    created_at: Date.now()
                };
                if (imageUrl) mascotObj.image_url = imageUrl;

                // Add to category
                if (!cat.mascots) cat.mascots = [];
                cat.mascots.push(mascotObj);

                // Clean up empty categories
                this.state.config.widget_config.categories = cats.filter(c => c.mascots && c.mascots.length > 0);

                this.closeModals();
                this.renderGrid();
                this.toast('Mascot saved locally. Remember to Publish!', 'success');
            },

            deleteMascot(id) {
                if (confirm(`Are you sure you want to delete mascot #${id}?`)) {
                    let cats = this.state.config.widget_config.categories;
                    cats.forEach(c => {
                        c.mascots = (c.mascots || []).filter(m => m.id !== id);
                    });
                    this.state.config.widget_config.categories = cats.filter(c => c.mascots && c.mascots.length > 0);
                    
                    this.renderGrid();
                    this.toast('Mascot deleted. Remember to Publish!', 'info');
                }
            }
        };

        window.addEventListener('DOMContentLoaded', () => app.init());
