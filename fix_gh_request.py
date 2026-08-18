import re

with open('admin.html', 'r') as f:
    content = f.read()

target = """            async ghRequest(endpoint, method = 'GET', body = null) {
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
            },"""

replacement = """            async ghRequest(endpoint, method = 'GET', body = null) {
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
                
                const responseText = await res.text();
                
                if (!res.ok) {
                    let errMsg = 'GitHub API request failed';
                    try {
                        if (responseText) {
                            const errData = JSON.parse(responseText);
                            errMsg = errData.message || errMsg;
                        }
                    } catch (e) {}
                    throw new Error(errMsg);
                }
                
                try {
                    return responseText ? JSON.parse(responseText) : null;
                } catch (e) {
                    return null;
                }
            },"""

if target in content:
    content = content.replace(target, replacement)
    with open('admin.html', 'w') as f:
        f.write(content)
    print("Fixed ghRequest")
else:
    print("Could not find target")
