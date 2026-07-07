import os
import re

def fix_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    new_lines = []
    for line in lines:
        if '<string name=' in line:
            # Find the content between > and <
            match = re.search(r'>(.*)</string>', line)
            if match:
                content = match.group(1)
                # Escape single quotes if not already escaped
                # But be careful not to escape already escaped ones
                # A simple way is to replace ' with \' but not \' with \'
                fixed_content = content.replace("\\'", "'").replace("'", "\\'")
                line = line.replace(content, fixed_content)
        new_lines.append(line)
    
    with open(filepath, 'w') as f:
        f.writelines(new_lines)

fix_file('app/src/main/res/values/strings.xml')
fix_file('app/src/main/res/values-ar/strings.xml')
fix_file('app/src/main/res/values-ar-rEG/strings.xml')
