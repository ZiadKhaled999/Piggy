import re

with open('./app/src/main/res/values/strings.xml', 'r') as f:
    content = f.read()

new_strings = """
    <string name="how_did_you_hear_about_us">How did you hear about Piggy Ledger?</string>
    <string name="source_facebook">Facebook</string>
    <string name="source_tiktok">TikTok</string>
    <string name="source_youtube">YouTube</string>
    <string name="source_friend_or_family">Friend or family</string>
    <string name="source_creator_or_influencer">Creator or influencer</string>
    <string name="source_search_engine">Search engine (e.g. Google)</string>
    <string name="source_google_play">Google Play</string>
    <string name="source_other">Other</string>
"""

content = content.replace('</resources>', new_strings + '</resources>')

with open('./app/src/main/res/values/strings.xml', 'w') as f:
    f.write(content)
