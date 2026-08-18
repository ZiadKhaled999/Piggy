import re

with open('admin.html', 'r') as f:
    content = f.read()

# Add chartInstances to state
content = content.replace("editingId: null,", "editingId: null,\n                chartInstances: {},")

js_analytics = """            renderAnalytics() {
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
            },"""

content = re.sub(r'renderAnalytics\(\) \{.*?(?=renderNotifGrid\(\) \{)', js_analytics + '\n\n            ', content, flags=re.DOTALL)

with open('admin.html', 'w') as f:
    f.write(content)
