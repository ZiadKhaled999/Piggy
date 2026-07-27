with open('app/src/main/res/values-ar-rEG/strings.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace occurrences of 'دفتر الحصالة' with 'فلوسك يا بيه' in Egyptian strings
updated = content.replace('دفتر الحصالة', 'فلوسك يا بيه')

with open('app/src/main/res/values-ar-rEG/strings.xml', 'w', encoding='utf-8') as f:
    f.write(updated)

print("Updated values-ar-rEG/strings.xml successfully!")
