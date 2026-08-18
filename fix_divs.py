import re

with open('admin.html', 'r') as f:
    content = f.read()

analytics_html = """        <!-- ANALYTICS SECTION -->
        <div id="section-analytics" class="hidden">
            <div class="toolbar">
                <div class="toolbar-row" style="justify-content: space-between;">
                    <div>
                        <h2 style="font-size: var(--text-lg); color: var(--color-surface-900);">Analytics Dashboard</h2>
                        <p style="font-size: var(--text-sm); color: var(--color-surface-500);">Overview of Piggy Studio data.</p>
                    </div>
                    <div>
                        <select id="analyticsPeriod" class="form-control" onchange="app.renderAnalytics()">
                            <option value="7">Last 7 Days</option>
                            <option value="30">Last 30 Days</option>
                            <option value="90">Last 90 Days</option>
                            <option value="all">All Time</option>
                        </select>
                    </div>
                </div>
            </div>

            <div class="mascot-grid" style="grid-template-columns: repeat(auto-fill, minmax(min(100%, 200px), 1fr)); margin-bottom: var(--space-6);">
                <div class="mascot-card" style="min-height: 140px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Total Widgets</h3>
                        <div style="font-size: 2.5rem; font-weight: 800; color: var(--color-brand-600); margin-top: var(--space-2);" id="stat-total-widgets">0</div>
                    </div>
                </div>
                <div class="mascot-card" style="min-height: 140px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Total Notifications</h3>
                        <div style="font-size: 2.5rem; font-weight: 800; color: var(--color-info); margin-top: var(--space-2);" id="stat-total-notifs">0</div>
                    </div>
                </div>
                <div class="mascot-card" style="min-height: 140px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Active Widgets</h3>
                        <div style="font-size: 2.5rem; font-weight: 800; color: var(--color-success); margin-top: var(--space-2);" id="stat-active-widgets">0</div>
                    </div>
                </div>
                <div class="mascot-card" style="min-height: 140px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Active Notifications</h3>
                        <div style="font-size: 2.5rem; font-weight: 800; color: var(--color-warning); margin-top: var(--space-2);" id="stat-active-notifs">0</div>
                    </div>
                </div>
            </div>

            <div class="mascot-grid" style="grid-template-columns: repeat(auto-fill, minmax(min(100%, 400px), 1fr)); margin-bottom: var(--space-6);">
                <div class="mascot-card" style="min-height: 320px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Widgets Classification (Status)</h3>
                        <div style="position: relative; height: 240px; width: 100%; margin-top: var(--space-2);">
                            <canvas id="chartWidgetStatus"></canvas>
                        </div>
                    </div>
                </div>
                <div class="mascot-card" style="min-height: 320px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Notifications Classification (Audience)</h3>
                        <div style="position: relative; height: 240px; width: 100%; margin-top: var(--space-2);">
                            <canvas id="chartNotifAudience"></canvas>
                        </div>
                    </div>
                </div>
                <div class="mascot-card" style="min-height: 320px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Widgets vs Notifications</h3>
                        <div style="position: relative; height: 240px; width: 100%; margin-top: var(--space-2);">
                            <canvas id="chartCompare"></canvas>
                        </div>
                    </div>
                </div>
                <div class="mascot-card" style="min-height: 320px;">
                    <div class="mascot-card-body">
                        <h3 class="mascot-card-title">Creation Over Time</h3>
                        <div style="position: relative; height: 240px; width: 100%; margin-top: var(--space-2);">
                            <canvas id="chartTime"></canvas>
                        </div>
                    </div>
                </div>
            </div>
        </div>"""

content = re.sub(r'<!-- ANALYTICS SECTION -->.*?</main>', analytics_html + '\n\n    </main>', content, flags=re.DOTALL)

with open('admin.html', 'w') as f:
    f.write(content)
