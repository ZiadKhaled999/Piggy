import re

for file in ['./app/src/main/res/values/strings.xml', './app/src/main/res/values-ar/strings.xml', './app/src/main/res/values-ar-rEG/strings.xml']:
    with open(file, 'r') as f:
        content = f.read()
    
    content = re.sub(r'<string name="onboarding_personalize_intensity_(casual|balanced)_desc">(.*?)</string>', 
                     r'<string name="onboarding_personalize_intensity_\1_desc" formatted="false">\2</string>', 
                     content)
    
    with open(file, 'w') as f:
        f.write(content)

