with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '        "Facebook" to "📱",\n        "TikTok" to "🎵",\n        "YouTube" to "▶️",\n        "Friend or family" to "😊",\n        "Creator or influencer" to "⭐",\n        "Search engine (e.g. Google)" to "🔍",\n        "Google Play" to "▶️",\n        "Other" to "✨"',
    '        stringResource(id = R.string.source_facebook) to "📱",\n        stringResource(id = R.string.source_tiktok) to "🎵",\n        stringResource(id = R.string.source_youtube) to "▶️",\n        stringResource(id = R.string.source_friend_or_family) to "😊",\n        stringResource(id = R.string.source_creator_or_influencer) to "⭐",\n        stringResource(id = R.string.source_search_engine) to "🔍",\n        stringResource(id = R.string.source_google_play) to "▶️",\n        stringResource(id = R.string.source_other) to "✨"'
)

content = content.replace(
    'text = "How did you hear about Piggy Ledger?"',
    'text = stringResource(id = R.string.how_did_you_hear_about_us)'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'w') as f:
    f.write(content)
