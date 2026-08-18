import re

with open('admin.html', 'r') as f:
    content = f.read()

# Let's count divs in section-analytics
match = re.search(r'(<!-- ANALYTICS SECTION -->.*?)</main>', content, flags=re.DOTALL)
if match:
    snippet = match.group(1)
    open_divs = len(re.findall(r'<div\b', snippet))
    close_divs = len(re.findall(r'</div>', snippet))
    print(f"Open divs: {open_divs}, Close divs: {close_divs}")
