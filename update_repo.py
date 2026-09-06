import re

with open('app/src/main/java/com/oryno/piggy_ledger/ai/AiChatRepository.kt', 'r') as f:
    content = f.read()

content = content.replace('@cf/meta/llama-3.1-8b-instruct', '@cf/meta/llama-3.1-8b-instruct-fp8')

with open('app/src/main/java/com/oryno/piggy_ledger/ai/AiChatRepository.kt', 'w') as f:
    f.write(content)
