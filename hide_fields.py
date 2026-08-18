import re

with open('admin.html', 'r') as f:
    content = f.read()

target = """                <div class="form-group">
                    <label>Personal Access Token (Classic with 'repo' scope)</label>
                    <input type="password" id="ghToken" class="form-control" placeholder="ghp_xxxxxxxxxxxx" />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Repository Owner (Username)</label>
                        <input type="text" id="ghOwner" class="form-control" placeholder="e.g. ZiadKhaled999" value="ZiadKhaled999" />
                    </div>
                    <div class="form-group">
                        <label>Repository Name</label>
                        <input type="text" id="ghRepo" class="form-control" placeholder="e.g. piggy-assets" value="piggy-assets" />
                    </div>
                </div>
                
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
                </div>"""

replacement = """                <div class="form-group">
                    <label>Personal Access Token (Classic with 'repo' scope)</label>
                    <input type="password" id="ghToken" class="form-control" placeholder="ghp_xxxxxxxxxxxx" />
                </div>
                <div class="form-row hidden">
                    <div class="form-group">
                        <label>Repository Owner (Username)</label>
                        <input type="text" id="ghOwner" class="form-control" placeholder="e.g. ZiadKhaled999" value="ZiadKhaled999" />
                    </div>
                    <div class="form-group">
                        <label>Repository Name</label>
                        <input type="text" id="ghRepo" class="form-control" placeholder="e.g. piggy-assets" value="piggy-assets" />
                    </div>
                </div>
                
                <div class="form-row hidden">
                    <div class="form-group">
                        <label>Branch</label>
                        <input type="text" id="ghBranch" class="form-control" value="main" />
                    </div>
                </div>
                <div class="form-row hidden">
                    <div class="form-group">
                        <label>Widgets File Path</label>
                        <input type="text" id="ghPath" class="form-control" value="piggy_remote_config.json" />
                    </div>
                    <div class="form-group">
                        <label>Notifications File Path</label>
                        <input type="text" id="ghNotifPath" class="form-control" value="notifications_config.json" />
                    </div>
                </div>"""

if target in content:
    content = content.replace(target, replacement)
    with open('admin.html', 'w') as f:
        f.write(content)
    print("Fields hidden successfully")
else:
    print("Could not find the target HTML to replace")

